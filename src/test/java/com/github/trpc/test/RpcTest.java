package com.github.trpc.test;

import com.github.trpc.client.RpcClient;
import com.github.trpc.client.RpcProxy;
import com.github.trpc.common.exception.RpcException;
import com.github.trpc.server.RpcServer;
import com.github.trpc.test.service.EchoService;
import com.github.trpc.test.service.EchoServiceImpl;
import com.github.trpc.test.service.User;
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
        rpcServer.start();

        log.info("start client");
        RpcClient rpcClient = new RpcClient("127.0.0.1", 8080);
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
