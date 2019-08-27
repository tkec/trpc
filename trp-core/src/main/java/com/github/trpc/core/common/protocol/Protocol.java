package com.github.trpc.core.common.protocol;

import com.github.trpc.core.common.DynamicCompositeByteBuf;
import io.netty.buffer.ByteBuf;

public interface Protocol {

    Request createRequest();

    Response createResponse();

    ByteBuf encodeRequest(Request request) throws Exception;

    Request decodeRequest(ByteBuf byteBuf) throws Exception;

    Request decodeRequest(DynamicCompositeByteBuf byteBuf) throws Exception;

    ByteBuf encodeResponse(Response response) throws Exception;

    Response decodeResponse(ByteBuf byteBuf) throws Exception;

    Response decodeResponse(DynamicCompositeByteBuf byteBuf) throws Exception;
}
