package com.github.trpc.core.client;

import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.common.protocol.Response;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class RpcTimeoutTimer implements TimerTask {

    private RpcClient rpcClient;
    private Long id;


    @Override
    public void run(Timeout timeout) throws Exception {
        RpcFuture rpcFuture = rpcClient.getRequestFutureMap().get(id);
        if (rpcFuture != null) {
            String errMsg = String.format("Request timeout, id=%d", id);
            log.error(errMsg);
            Response response = rpcClient.getProtocol().createResponse();
            response.setException(new RpcException(RpcException.TIMEOUT_EXCEPTION, errMsg));
            rpcFuture.handleResponse(response);
        }
    }
}
