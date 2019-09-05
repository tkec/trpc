package com.github.trpc.example.test;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.RpcProxy;
import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.server.RpcServer;
import com.github.trpc.example.benchmark.service.EchoService;
import com.github.trpc.example.benchmark.service.EchoServiceImpl;
import com.github.trpc.example.test.service.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcTest {

    public static void main(String[] args) {
        log.info("start server");
        RpcServer rpcServer = new RpcServer(8081);
        EchoServiceImpl echoService = new EchoServiceImpl();
        rpcServer.registerService(echoService);
        rpcServer.start();

        log.info("start client");
        RpcClient rpcClient = new RpcClient("127.0.0.1", 8081);
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
