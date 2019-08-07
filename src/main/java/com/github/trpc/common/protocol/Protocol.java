package com.github.trpc.common.protocol;

import io.netty.buffer.ByteBuf;

public interface Protocol {

    Request createRequest();

    Response createResponse();

    ByteBuf encodeRequest(Request request) throws Exception;

    Request decodeRequest(ByteBuf byteBuf) throws Exception;

    ByteBuf encodeResponse(Response response) throws Exception;

    Response decodeResponse(ByteBuf byteBuf) throws Exception;
}
