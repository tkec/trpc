package com.github.trpc.springboot.annotation;

import com.github.trpc.core.server.RpcServer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Slf4j
public class RpcServiceExporter implements DisposableBean {

    private RpcServer rpcServer;

    private Integer port;

    private List<Object> registerService = new ArrayList<>();


    @Override
    public void destroy() throws Exception {
        if (rpcServer != null) {
            rpcServer.shutdown();
        }
    }

    public void start() throws Exception {
        Assert.isTrue(port > 0, "invalid service port");
        Assert.isTrue(registerService.size() > 0, "no register service");

        rpcServer = new RpcServer(port);
        for (Object service : registerService) {
            rpcServer.registerService(service);
        }

        rpcServer.start();
        log.info("RpcServer in spring start at port:" + port);
    }
}
