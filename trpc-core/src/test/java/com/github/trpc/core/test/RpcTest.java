package com.github.trpc.core.test;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.RpcProxy;
import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.common.protocol.rpcprotocol.RpcProtocol;
import com.github.trpc.core.test.service.EchoServiceImpl;
import com.github.trpc.core.test.service.User;
import com.github.trpc.core.server.RpcServer;
import com.github.trpc.core.test.service.EchoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class RpcTest {

    @Test
    public void test1() {
        log.info("start server");
        RpcServer rpcServer = new RpcServer(8080);
        EchoServiceImpl echoService = new EchoServiceImpl();
        rpcServer.registerService(echoService);
        rpcServer.setProtocol(new RpcProtocol());
        rpcServer.start();

        log.info("start client");
        RpcClient rpcClient = new RpcClient("127.0.0.1", 8080);
        rpcClient.setProtocol(new RpcProtocol());
        EchoService echoServiceProxy = RpcProxy.getProxy(rpcClient, EchoService.class);
        String result = echoServiceProxy.echo("Hello Rpc");
        log.info("result from server: " + result);
        try {
            echoServiceProxy.exception("exception");
        } catch (RpcException e) {
            log.error("exception from server: " + e.getMessage());
        }

        User user = echoServiceProxy.getUser("user1", 20);
        log.info("client receive user: " + user);

        log.info("shutdown client");
        rpcClient.shutdown();
        log.info("shutdown server");
        rpcServer.shutdown();
    }
}
