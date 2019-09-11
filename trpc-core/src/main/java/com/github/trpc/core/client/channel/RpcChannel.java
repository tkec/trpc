package com.github.trpc.core.client.channel;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.instance.ServiceInstance;
import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.common.thread.CustomThreadFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Setter
@Getter
public class RpcChannel {
    private ServiceInstance serviceInstance;
    private RpcClient rpcClient;
    private volatile Channel channel;

    private ExecutorService executorService = Executors.newFixedThreadPool(3,
            new CustomThreadFactory("channel-reconnect-thread"));

    public RpcChannel(ServiceInstance serviceInstance, RpcClient rpcClient) {
        this.serviceInstance = serviceInstance;
        this.rpcClient = rpcClient;
    }

    public Channel getChannel() throws Exception {
        if (!isActive(channel)) {
            synchronized (this) {
                if (!isActive(channel)) {
                    if (channel != null) {
                        channel.close();
                    }
                    channel = createChannel(serviceInstance.getHost(), serviceInstance.getPort());
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
        final ChannelFuture future = rpcClient.getBootstrap().connect(new InetSocketAddress(ip, port));
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

    public void removeChannel() {
        executorService.submit(() -> {
            if (channel != null) {
                channel.close();
            }
            channel = createChannel(serviceInstance.getHost(), serviceInstance.getPort());
        });
    }

    public void close() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }
}
