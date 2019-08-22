package com.github.trpc.common.protocol;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class AbstractRpcProtocol implements Protocol {

    @Override
    public Request createRequest() {
        return new RpcRequest();
    }

    @Override
    public Response createResponse() {
        return new RpcResponse();
    }

}
