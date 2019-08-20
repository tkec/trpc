package com.github.trpc.client.handler;

import com.github.trpc.client.RpcClient;
import com.github.trpc.client.RpcFuture;
import com.github.trpc.common.protocol.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AllArgsConstructor
public class ClientWorkTask implements Runnable {

    private RpcClient rpcClient;
    private Response response;

    @Override
    public void run() {
        // log.info("Client work thread, response:" + response);
        if (response == null || rpcClient == null) {
            return;
        }

        ConcurrentHashMap<Long, RpcFuture> requestFutureMap = rpcClient.getRequestFutureMap();
        Long id = response.getId();
        RpcFuture future = requestFutureMap.get(id);
        if (future == null) {
            log.error("future is null, RpcServer return slow, id=" + id);
        } else {
            future.handleResponse(response);
        }
    }
}
