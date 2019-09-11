package com.github.trpc.core.server;

import com.github.trpc.core.common.extension.ExtensionLoaderManager;
import com.github.trpc.core.common.protocol.Protocol;
import com.github.trpc.core.common.protocol.protorpcprotocol.ProtoRpcProtocol;
import com.github.trpc.core.common.protocol.rpcprotocol.RpcProtocol;
import com.github.trpc.core.common.registry.*;
import com.github.trpc.core.common.thread.CustomThreadFactory;
import com.github.trpc.core.common.utils.NetUtils;
import com.github.trpc.core.server.handler.RpcServerChannelIdleHandler;
import com.github.trpc.core.server.handler.RpcServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.NetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@Slf4j
public class RpcServer {

    private ServerBootstrap bootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup ioGroup;
    private int port;
    private RpcServerConfig rpcServerConfig;
    private Registry registry;
    private List<RegisterInfo> registerInfoList = new ArrayList<>();
    private Protocol protocol;
    private ExecutorService workThreadPool;
    private AtomicBoolean start = new AtomicBoolean(false);


    public RpcServer(int port) {
        this(port, 100, new RpcServerConfig());
    }

    public RpcServer(int port ,int workThreadNum) {
        this(port, workThreadNum, new RpcServerConfig());
    }

    public RpcServer(int port ,int workThreadNum, RpcServerConfig rpcServerConfig) {
        this.port = port;
        protocol = new RpcProtocol();
        ExtensionLoaderManager.getInstance().loadAllExtensions();
        if (StringUtils.isNotEmpty(rpcServerConfig.getRegistryUrl())) {
            RpcURL url = new RpcURL(rpcServerConfig.getRegistryUrl());
            RegistryFactory registryFactory = RegistryFactoryManager.getInstance().getRegistryFactory(url.getSchema());
            this.registry = registryFactory.createRegistry(url);
        }

        bootstrap = new ServerBootstrap();
        int ioNum = Runtime.getRuntime().availableProcessors();
        bossGroup = new NioEventLoopGroup(1, new CustomThreadFactory("server-accept-thread"));
        ioGroup = new NioEventLoopGroup(ioNum, new CustomThreadFactory("server-io-thread"));
        ((NioEventLoopGroup) bossGroup).setIoRatio(100);
        ((NioEventLoopGroup)ioGroup).setIoRatio(100);
        bootstrap.channel(NioServerSocketChannel.class);
        workThreadPool = Executors.newFixedThreadPool(workThreadNum, new CustomThreadFactory("server-work-thread"));

        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.SO_LINGER, 5);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, 1024 * 64 * 1024);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, 1024 * 64 * 1024);

        final RpcServerHandler rpcServerHandler = new RpcServerHandler(this);
        ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                // ch.pipeline().addLast("logging", new LoggingHandler(LogLevel.INFO));
                ch.pipeline().addLast("idleStateAwareHandler", new IdleStateHandler(60, 60, 60));
                ch.pipeline().addLast("idle", new RpcServerChannelIdleHandler());
                ch.pipeline().addLast(rpcServerHandler);
            }
        };
        bootstrap.group(bossGroup, ioGroup).childHandler(channelInitializer);

        // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.shutdown()));
    }

    public void registerService(Object service) {
        ServiceManager.getInstance().registerService(service);

        RegisterInfo registerInfo = new RegisterInfo();
        registerInfo.setHost(NetUtils.getLocalAddress().getHostAddress());
        registerInfo.setPort(port);
        registerInfo.setInterfaceName(service.getClass().getInterfaces()[0].getName());
        registerInfoList.add(registerInfo);
    }

    public void start() {
        if (start.compareAndSet(false, true)) {
            try {
                ChannelFuture channelFuture = bootstrap.bind(port);
                channelFuture.sync();
                if (registry != null) {
                    for (RegisterInfo registerInfo : registerInfoList) {
                        registry.register(registerInfo);
                    }
                }
            } catch (Exception e) {
                log.error("Server start error: " + e.getLocalizedMessage());
                return;
            }
            log.info("Server start success, port:" + port);
        }
    }

    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (ioGroup != null) {
            ioGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (workThreadPool != null) {
            workThreadPool.shutdown();
        }
    }
}
