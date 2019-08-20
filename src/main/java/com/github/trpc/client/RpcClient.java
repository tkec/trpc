package com.github.trpc.client;

import com.github.trpc.client.handler.IdleChannelHandler;
import com.github.trpc.client.handler.RpcClientHandler;
import com.github.trpc.common.exception.RpcException;
import com.github.trpc.common.protocol.Protocol;
import com.github.trpc.common.protocol.Request;
import com.github.trpc.common.protocol.RpcProtocol;
import com.github.trpc.common.thread.ClientTimeoutTimerInstance;
import com.github.trpc.common.thread.CustomThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.internal.logging.InternalLogLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Getter
@Setter
public class RpcClient {

    private Bootstrap bootstrap;
    private Timer timeoutTimer;
    private Protocol protocol;
    private ExecutorService workThreadPool;
    private EventLoopGroup ioThreadPool;
    private ConcurrentHashMap<Long, RpcFuture> requestFutureMap;
    private Class serviceInterface;
    private Endpoint endpoint;
    private Channel channel;
    private LongAdder idGen = new LongAdder();
    // private AtomicLong idGen = new AtomicLong();
    private AtomicBoolean start = new AtomicBoolean(false);


    public RpcClient(String host, Integer port) {
        this(new Endpoint(host, port));
    }

    public RpcClient(Endpoint endpoint) {
        protocol = new RpcProtocol();
        timeoutTimer = ClientTimeoutTimerInstance.getInstance();
        this.endpoint = endpoint;
        int threadNum = Runtime.getRuntime().availableProcessors();
        workThreadPool = Executors.newFixedThreadPool(threadNum, new CustomThreadFactory("client-work-thread"));
        ioThreadPool = new NioEventLoopGroup(threadNum, new CustomThreadFactory("client-io-thread"));
        requestFutureMap = new ConcurrentHashMap<>();

        bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_RCVBUF, 1024 * 64);
        bootstrap.option(ChannelOption.SO_SNDBUF, 1024 * 64);

        final RpcClientHandler rpcClientHandler = new RpcClientHandler(this);
        final ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                // ch.pipeline().addLast("logging", new LoggingHandler(LogLevel.INFO));
                ch.pipeline().addLast(new IdleStateHandler(0, 0, 60));
                ch.pipeline().addLast(new IdleChannelHandler());
                ch.pipeline().addLast(rpcClientHandler);
            }
        };
        // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        bootstrap.group(ioThreadPool).handler(initializer);
    }

    public Channel selectChannel(Request request) {
        if (!isActive(channel)) {
            synchronized (this) {
                if (!isActive(channel)) {
                    log.info("reconnect to server");
                    Channel newChannel = createChannel(endpoint.getHost(), endpoint.getPort());
                    if (channel != null ) {
                        channel.close();
                    }
                    channel = newChannel;
                }
            }
        }
        return channel;
    }

    private Boolean isActive(Channel channel) {
        return channel != null && channel.isActive();
    }

    private Channel createChannel(String ip, Integer port) {
        Channel channel;
        channel = connect(ip, port);
        return channel;
    }

    private Channel connect(final String ip, final int port) {
        final ChannelFuture future = bootstrap.connect(new InetSocketAddress(ip, port));
        future.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                log.debug("future callback, connect to {}:{} success, channel={}",
                        ip, port, channelFuture.channel());
            } else {
                log.debug("future callback, connect to {}:{} failed due to {}",
                        ip, port, channelFuture.cause().getMessage());
            }
        });
        future.syncUninterruptibly();
        if (future.isSuccess()) {
            return future.channel();
        } else {
            // throw exception when connect failed to the connection pool acquirer
            log.error("connect to {}:{} failed, msg={}", ip, port, future.cause().getMessage());
            throw new RpcException(future.cause());
        }
    }

    public RpcFuture sendRequest(Request request, Channel channel) {
        RpcFuture rpcFuture = new RpcFuture();
//        rpcFuture.setRpcClient(this);

        idGen.increment();
        Long id = idGen.longValue();
     //   Long id = idGen.incrementAndGet();
        request.setId(id);
        requestFutureMap.put(id, rpcFuture);
        RpcTimeoutTimer rpcTimeoutTimer = new RpcTimeoutTimer(this, id);
        Timeout timeout = timeoutTimer.newTimeout(rpcTimeoutTimer, 200000, TimeUnit.MILLISECONDS);
        rpcFuture.setTimeout(timeout);

        try {
            ByteBuf byteBuf = protocol.encodeRequest(request);
            log.debug("send result to server. id=" + request.getId()
                    + ", channel=" + channel + ", active=" + channel.isActive());
            ChannelFuture sendFuture = channel.writeAndFlush(byteBuf);
            sendFuture.awaitUninterruptibly();
            if (!sendFuture.isSuccess()) {
                String errMsg = String.format("Send request failed, channel=%s, active=%d, ex=%s",
                        channel.toString(), channel.isActive(), sendFuture.cause().getMessage());
                throw new RpcException(RpcException.NETWORK_EXCEPTION, errMsg);
            }
            log.debug("send request: " + request);
        } catch (Exception e) {
            timeout.cancel();
            if (e instanceof RpcException) {
                throw (RpcException) e;
            } else {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage());
            }
        }
        return rpcFuture;
    }

    public void shutdown() {
        if (start.compareAndSet(true, false)) {
            if (workThreadPool != null) {
                workThreadPool.shutdown();
            }
            if (ioThreadPool != null) {
                ioThreadPool.shutdownGracefully();
            }
            if (channel != null) {
                channel.close();
            }
            if (timeoutTimer != null) {
                timeoutTimer.stop();
            }
        }
    }
}
