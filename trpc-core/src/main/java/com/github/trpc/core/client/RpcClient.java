package com.github.trpc.core.client;

import com.github.trpc.core.client.channel.RpcChannel;
import com.github.trpc.core.client.handler.IdleChannelHandler;
import com.github.trpc.core.client.handler.RpcClientHandler;
import com.github.trpc.core.client.instance.BasicInstanceProcessor;
import com.github.trpc.core.client.instance.Endpoint;
import com.github.trpc.core.client.instance.InstanceProcessor;
import com.github.trpc.core.client.instance.ServiceInstance;
import com.github.trpc.core.client.loadbalance.LoadBalanceManager;
import com.github.trpc.core.client.loadbalance.LoadBalanceStrategy;
import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.common.extension.ExtensionLoaderManager;
import com.github.trpc.core.common.protocol.Protocol;
import com.github.trpc.core.common.protocol.Request;
import com.github.trpc.core.common.protocol.rpcprotocol.RpcProtocol;
import com.github.trpc.core.common.registry.*;
import com.github.trpc.core.common.thread.ClientTimeoutTimerInstance;
import com.github.trpc.core.common.thread.CustomThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Getter
@Setter
public class RpcClient {

    private Bootstrap bootstrap;
    private Timer timeoutTimer;
    private Protocol protocol;
    private RpcClientConfig rpcClientConfig = new RpcClientConfig();
    private Registry registry;
    private SubscribeInfo subscribeInfo;
    private ExecutorService workThreadPool;
    private EventLoopGroup ioThreadPool;
    private ConcurrentHashMap<Long, RpcFuture> requestFutureMap;
    private Class serviceInterface;
    private InstanceProcessor instanceProcessor;
    private LoadBalanceStrategy loadBalanceStrategy;
//    private Endpoint endpoint;
//    private Channel channel;
    private AtomicLong idGen = new AtomicLong();
    private AtomicBoolean start = new AtomicBoolean(false);

    public RpcClient(String serverUrl) {
        this(serverUrl, new RpcClientConfig());
    }

    public RpcClient(String serverUrl, RpcClientConfig rpcClientConfig) {
        Validate.notEmpty(serverUrl);
        if (rpcClientConfig == null) {
            rpcClientConfig = new RpcClientConfig();
        }

        ExtensionLoaderManager.getInstance().loadAllExtensions();

        RpcURL url = new RpcURL(serverUrl);
        RegistryFactory registryFactory = RegistryFactoryManager.getInstance().getRegistryFactory(url.getSchema());
        this.registry = registryFactory.createRegistry(url);
        init(rpcClientConfig);
    }

    public RpcClient(String host, Integer port) {
        this(new Endpoint(host, port));
    }

    public RpcClient(Endpoint endpoint) {
        this(endpoint, new RpcClientConfig());
    }

    public RpcClient(Endpoint endpoint, RpcClientConfig rpcClientConfig) {
        Validate.notNull(endpoint);
        if (rpcClientConfig == null) {
            rpcClientConfig = new RpcClientConfig();
        }
//        this.endpoint = endpoint;
        init(rpcClientConfig);
        instanceProcessor.addInstance(new ServiceInstance(endpoint));
    }

    private void init(final RpcClientConfig rpcClientConfig) {
        this.rpcClientConfig.copyFrom(rpcClientConfig);
        protocol = new RpcProtocol();
        ExtensionLoaderManager.getInstance().loadAllExtensions();

        instanceProcessor = new BasicInstanceProcessor(this);
        loadBalanceStrategy = LoadBalanceManager.getInstance().createLoadBalance(
                this.rpcClientConfig.getLoadBalanceType());
        loadBalanceStrategy.init(this);

        timeoutTimer = ClientTimeoutTimerInstance.getInstance();

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
        bootstrap.option(ChannelOption.SO_RCVBUF, 1024 * 64 * 1064);
        bootstrap.option(ChannelOption.SO_SNDBUF, 1024 * 64 * 1064);

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

    public void addServiceInterface(Class serviceInterface) {
        if (this.serviceInterface != null) {
            throw new IllegalArgumentException("RpcClient serviceInterface has set");
        }
        this.serviceInterface = serviceInterface;

        if (registry != null) {
            subscribeInfo = new SubscribeInfo();
            subscribeInfo.setInterfaceName(serviceInterface.getName());

            List<ServiceInstance> instances = registry.lookup(subscribeInfo);
            // add instances
            instanceProcessor.addInstances(instances);
            registry.subscribe(subscribeInfo, (addList, deleteList) -> {
                instanceProcessor.addInstances(addList);
                instanceProcessor.deleteInstances(deleteList);
            });
        }
    }

    public Channel selectChannel(Request request) {
        RpcChannel rpcChannel = loadBalanceStrategy.selectInstance(request,
                instanceProcessor.getRpcChannels(), null);
        if (rpcChannel == null) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "no available instance");
        }
        Channel channel = null;
        try {
            channel = rpcChannel.getChannel();
        } catch (Exception e) {
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, "channel connect failed");
        }
        if (channel == null) {
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, "channel is null");
        }
        if (!channel.isActive()) {
            rpcChannel.removeChannel();
            throw new RpcException(RpcException.UNKNOWN_EXCEPTION, "channel is inactive");
        }
        return channel;
//        if (!isActive(channel)) {
//            synchronized (this) {
//                if (!isActive(channel)) {
//                    log.info("reconnect to server");
//                    Channel newChannel = createChannel(endpoint.getHost(), endpoint.getPort());
//                    if (channel != null ) {
//                        channel.close();
//                    }
//                    channel = newChannel;
//                }
//            }
//        }
//        return channel;
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

//        idGen.increment();
//        Long id = idGen.longValue();
        Long id = idGen.incrementAndGet();
        request.setId(id);
        RpcFuture rpcFuture1 = requestFutureMap.get(id);
        if (rpcFuture1 != null) {
            log.error("RpcFuture is exist, id=" + id);
            return rpcFuture;
        }
        requestFutureMap.put(id, rpcFuture);
        RpcTimeoutTimer rpcTimeoutTimer = new RpcTimeoutTimer(this, id);
        Timeout timeout = timeoutTimer.newTimeout(rpcTimeoutTimer, 20000, TimeUnit.MILLISECONDS);
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
            e.printStackTrace();
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
//            if (channel != null) {
//                channel.close();
//            }
            if (timeoutTimer != null) {
                timeoutTimer.stop();
            }
            if (registry != null) {
                registry.unSubscribe(subscribeInfo);
            }
            if (instanceProcessor != null) {
                instanceProcessor.stop();
            }
            if (loadBalanceStrategy != null) {
                loadBalanceStrategy.destory();
            }
        }
    }
}
