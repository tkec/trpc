package com.github.trpc.common.protocol;

import com.github.trpc.common.exception.BadSchemaException;
import com.github.trpc.common.exception.NotEnoughDataException;
import com.github.trpc.common.exception.TooBigDataException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Header:
 * [TRPC][body-size][meta-size]
 */
@Slf4j
public class RpcProtocol implements Protocol {

    private static final byte[] MAGIC_HEAD = "TRPC".getBytes();
    private static final int FIXED_LEN = 12;


    @Override
    public Request createRequest() {
        return new RpcRequest();
    }

    @Override
    public Response createResponse() {
        return new RpcResponse();
    }

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        RpcProto.RpcMeta.Builder rpcMetaBuilder = RpcProto.RpcMeta.newBuilder();
        RpcProto.RpcRequestMeta.Builder rpcRequestMetaBuilder = RpcProto.RpcRequestMeta.newBuilder();
        rpcMetaBuilder.setId(request.getId());
        rpcRequestMetaBuilder.setMethodName(request.getMethodName());
        rpcRequestMetaBuilder.setServiceName(request.getServiceName());
        rpcMetaBuilder.setRequest(rpcRequestMetaBuilder.build());

        ByteBuf headBuf = Unpooled.buffer(FIXED_LEN);
        headBuf.writeBytes(MAGIC_HEAD);
        byte[] metaBytes = rpcMetaBuilder.build().toByteArray();
        ByteBuf metaBuf = Unpooled.wrappedBuffer(metaBytes);
        int metaSize = metaBytes.length;
        int bodySize = metaSize;
        ByteBuf bodyBuf = null;
        if (request.getArgs() != null) {
            bodyBuf = transObjToByteBuf(request.getArgs());
            bodySize += bodyBuf.readableBytes();
        }
        headBuf.writeInt(bodySize);
        headBuf.writeInt(metaSize);

        if (bodyBuf != null) {
            return Unpooled.wrappedBuffer(headBuf, metaBuf, bodyBuf);
        } else {
            return Unpooled.wrappedBuffer(headBuf, metaBuf);
        }
    }

    private ByteBuf transObjToByteBuf(Object object) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            byte[] argsBytes = byteArrayOutputStream.toByteArray();
            return Unpooled.wrappedBuffer(argsBytes);
        } finally {
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {

                }
            }
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {

                }
            }
        }
    }

    @Override
    public Request decodeRequest(ByteBuf byteBuf) throws Exception {
        Map<String, Object> rpcMap = getRpcMap(byteBuf);
        RpcProto.RpcMeta rpcMeta = (RpcProto.RpcMeta)rpcMap.get("rpcMeta");
        Object body = rpcMap.get("body");

        Request request = new RpcRequest();

        RpcProto.RpcRequestMeta rpcRequestMeta = rpcMeta.getRequest();
        Object[] args = (Object[])body;

        request.setId(rpcMeta.getId());
        request.setServiceName(rpcRequestMeta.getServiceName());
        request.setMethodName(rpcRequestMeta.getMethodName());
        request.setArgs(args);

        log.debug("decode request: " + request);
        return request;
    }

    @Override
    public ByteBuf encodeResponse(Response response) throws Exception {
        RpcProto.RpcMeta.Builder rpcMetaBuilder = RpcProto.RpcMeta.newBuilder();
        rpcMetaBuilder.setId(response.getId());
        RpcProto.RpcResponseMeta.Builder rpcResponseMetaBuilder = RpcProto.RpcResponseMeta.newBuilder();
        ByteBuf headBuf = Unpooled.buffer(FIXED_LEN);
        headBuf.writeBytes(MAGIC_HEAD);
        ByteBuf metaByteBuf = null;
        ByteBuf bodyByteBuf = null;
        if (response.getException() != null) {
            rpcResponseMetaBuilder.setErrorCode(-1);
            rpcResponseMetaBuilder.setErrorMsg(response.getException().getLocalizedMessage());
            rpcMetaBuilder.setResponse(rpcResponseMetaBuilder.build());
        } else {
            rpcResponseMetaBuilder.setErrorCode(0);
            rpcMetaBuilder.setResponse(rpcResponseMetaBuilder.build());
            bodyByteBuf = transObjToByteBuf(response.getResult());
        }
        byte[] bytes = rpcMetaBuilder.build().toByteArray();
        metaByteBuf = Unpooled.wrappedBuffer(bytes);
        int metaSize = metaByteBuf.readableBytes();
        int bodySize = metaSize;
        if (bodyByteBuf != null) {
            bodySize += bodyByteBuf.readableBytes();
        }

        headBuf.writeInt(bodySize);
        headBuf.writeInt(metaSize);


        if (bodyByteBuf != null) {
            return Unpooled.wrappedBuffer(headBuf, metaByteBuf, bodyByteBuf);
        } else {
            return Unpooled.wrappedBuffer(headBuf, metaByteBuf);
        }
    }

    @Override
    public Response decodeResponse(ByteBuf byteBuf) throws Exception {
        Map<String, Object> rpcMap = getRpcMap(byteBuf);
        RpcProto.RpcMeta rpcMeta = (RpcProto.RpcMeta)rpcMap.get("rpcMeta");
        Object body = rpcMap.get("body");

        Response response = new RpcResponse();
        RpcProto.RpcResponseMeta rpcResponseMeta = rpcMeta.getResponse();

        response.setId(rpcMeta.getId());
        int errorCode = rpcResponseMeta.getErrorCode();
        if (errorCode != 0) {
            response.setException(new Exception(rpcResponseMeta.getErrorMsg()));
        }
        response.setResult(body);
        log.debug("decode response: " + response);
        return response;
    }

    private Object getObjectByByteBuf(ByteBuf byteBuf) throws Exception {
        final int len = byteBuf.readableBytes();
        byte[] bytes = new byte[len];
        byteBuf.readBytes(bytes, 0, len);

        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object object = objectInputStream.readObject();
            return object;
        } finally {
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (IOException e) {

                }
            }
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {

                }
            }
        }
    }

    /**
     * 解析header及body
     * @param byteBuf
     * @return
     * @throws Exception
     */
    private Map<String, Object> getRpcMap(ByteBuf byteBuf) throws Exception {
        if (byteBuf.readableBytes() < FIXED_LEN) {
            throw new NotEnoughDataException();
        }
        ByteBuf headBuf = byteBuf.readBytes(FIXED_LEN);
        byte[] magic = new byte[4];
        headBuf.readBytes(magic);
        if (!Arrays.equals(magic, MAGIC_HEAD)) {
            throw new BadSchemaException();
        }
        int bodySize = headBuf.readInt();
        if (byteBuf.readableBytes() < bodySize) {
            throw new NotEnoughDataException();
        }
        if (bodySize > 512 * 1024 * 1024) {
            throw new TooBigDataException();
        }
        int metaSize = headBuf.readInt();
        log.debug("head info, magic={}, bodySize={}, metaSize={}", new String(magic), bodySize, metaSize);
        if (metaSize > bodySize) {
            throw new BadSchemaException();
        }
        ByteBuf metaByteBuf = byteBuf.readBytes(metaSize);
        ByteBuf bodyByteBuf = null;
        Object body = null;
        RpcProto.RpcMeta rpcMeta = null;

        try {
            if (bodySize > metaSize) {
                bodyByteBuf = byteBuf.readBytes(bodySize - metaSize);
                body = getObjectByByteBuf(bodyByteBuf);
            }
            final int len = metaByteBuf.readableBytes();
            byte[] metaBytes = new byte[len];
            metaByteBuf.readBytes(metaBytes, 0, len);
            rpcMeta = RpcProto.RpcMeta.getDefaultInstance()
                    .getParserForType().parseFrom(metaBytes);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadSchemaException();
        }

        Map<String, Object> rpcMap = new HashMap<>();
        rpcMap.put("rpcMeta", rpcMeta);
        rpcMap.put("body", body);
        return rpcMap;
    }
}
