package com.github.trpc.client;

import com.github.trpc.common.exception.RpcException;
import com.github.trpc.common.protocol.Response;
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
            String host = rpcClient.getEndpoint().getHost();
            Integer port = rpcClient.getEndpoint().getPort();
            String errMsg = String.format("Request timeout, id=%d, ip=%s, port=%d.", id, host, port);
            log.error(errMsg);
            Response response = rpcClient.getProtocol().createResponse();
            response.setException(new RpcException(RpcException.TIMEOUT_EXCEPTION, errMsg));
            rpcFuture.handleResponse(response);
        }
    }
}
