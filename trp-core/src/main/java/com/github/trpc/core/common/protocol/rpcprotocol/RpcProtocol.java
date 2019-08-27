package com.github.trpc.core.common.protocol.rpcprotocol;

import com.github.trpc.core.common.DynamicCompositeByteBuf;
import com.github.trpc.core.common.exception.BadSchemaException;
import com.github.trpc.core.common.exception.NotEnoughDataException;
import com.github.trpc.core.common.exception.TooBigDataException;
import com.github.trpc.core.common.protocol.AbstractRpcProtocol;
import com.github.trpc.core.common.protocol.Request;
import com.github.trpc.core.common.protocol.Response;
import com.github.trpc.core.common.protocol.RpcRequest;
import com.github.trpc.core.common.protocol.protorpcprotocol.RpcProto;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Header:
 * [CPRT][body-size]|[body]
 */
@Slf4j
public class RpcProtocol extends AbstractRpcProtocol {

    private static final byte[] MAGIC_HEAD = "CPRT".getBytes();
    private static final int FIXED_LEN = 8;

    @Override
    public ByteBuf encodeRequest(Request request) throws Exception {
        ByteBuf headBuf = Unpooled.buffer(FIXED_LEN);
        headBuf.writeBytes(MAGIC_HEAD);
        ByteBuf bodyBuf = transObjToByteBuf(request);
        int bodySize = bodyBuf.readableBytes();
        headBuf.writeInt(bodySize);

        return Unpooled.wrappedBuffer(headBuf, bodyBuf);
    }

    @Override
    public ByteBuf encodeResponse(Response response) throws Exception {
        ByteBuf headBuf = Unpooled.buffer(FIXED_LEN);
        headBuf.writeBytes(MAGIC_HEAD);
        ByteBuf bodyBuf = transObjToByteBuf(response);
        int bodySize = bodyBuf.readableBytes();
        headBuf.writeInt(bodySize);

        return Unpooled.wrappedBuffer(headBuf, bodyBuf);
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

        Request request = this.createRequest();

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
    public Response decodeResponse(ByteBuf byteBuf) throws Exception {
        Map<String, Object> rpcMap = getRpcMap(byteBuf);
        RpcProto.RpcMeta rpcMeta = (RpcProto.RpcMeta)rpcMap.get("rpcMeta");
        Object body = rpcMap.get("body");

        Response response = this.createResponse();
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
        } catch (Exception e) {
           e.printStackTrace();
           return null;
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
            log.error("in head readable not enough: " + byteBuf.readableBytes());
            throw new NotEnoughDataException();
        }
        ByteBuf headBuf = byteBuf.readBytes(FIXED_LEN);
        ByteBuf metaByteBuf = null;
        ByteBuf bodyByteBuf = null;
        try {
            byte[] magic = new byte[4];
            headBuf.readBytes(magic);
            if (!Arrays.equals(magic, MAGIC_HEAD)) {
                log.warn("bad schema, get: {}, need: {}", new String(magic), "TRPC");
                throw new BadSchemaException();
            }
            int bodySize = headBuf.readInt();
            if (byteBuf.readableBytes() < bodySize) {
                log.error("in body readable not enough: " + byteBuf.readableBytes());
                throw new NotEnoughDataException();
            }
            if (bodySize > 512 * 1024 * 1024) {
                log.error("in body len bad, bodySize: " + bodySize);
                throw new TooBigDataException();
            }
            int metaSize = headBuf.readInt();
            log.debug("head info, magic={}, bodySize={}, metaSize={}", new String(magic), bodySize, metaSize);
            if (metaSize > bodySize) {
                log.error("in body bad, bodySize: " + bodySize + ", metaSize:" + metaSize);
                throw new BadSchemaException();
            }
            metaByteBuf = byteBuf.readBytes(metaSize);
            bodyByteBuf = null;
            Object body = null;
            RpcProto.RpcMeta rpcMeta = null;

            try {
                if (bodySize > metaSize) {
                    bodyByteBuf = byteBuf.readRetainedSlice(bodySize - metaSize);
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
        } finally {
            if (headBuf != null) {
                headBuf.release();
            }
            if (metaByteBuf != null) {
                metaByteBuf.release();
            }
            if (bodyByteBuf != null) {
                bodyByteBuf.release();
            }
        }
    }


    @Override
    public Request decodeRequest(DynamicCompositeByteBuf byteBuf) throws Exception {
        Object body = getRpcBody(byteBuf);
        Request request = (RpcRequest)body;
        log.debug("decode request: " + request);
        return request;
    }

    @Override
    public Response decodeResponse(DynamicCompositeByteBuf byteBuf) throws Exception {
        Object body = getRpcBody(byteBuf);

        Response response = (Response)body;
        log.debug("decode response: " + response);
        return response;
    }

    /**
     * 解析header及body
     * @param byteBuf
     * @return
     * @throws Exception
     */
    private Object getRpcBody(DynamicCompositeByteBuf byteBuf) throws Exception {
        if (byteBuf.readableBytes() < FIXED_LEN) {
            log.debug("in head readable not enough: " + byteBuf.readableBytes());
            throw new NotEnoughDataException();
        }
        // 使用retainedSlice，不更新readerIndex
        ByteBuf headBuf = byteBuf.retainedSlice(FIXED_LEN);
        ByteBuf bodyByteBuf = null;
        try {
            byte[] magic = new byte[4];
            headBuf.readBytes(magic);
            if (!Arrays.equals(magic, MAGIC_HEAD)) {
                log.warn("bad schema, get: {}, need: {}", new String(magic), "CPRT");
                throw new BadSchemaException();
            }
            int bodySize = headBuf.readInt();
            if (byteBuf.readableBytes() < bodySize + FIXED_LEN) {
                log.debug("in body readable not enough: " + byteBuf.readableBytes());
                throw new NotEnoughDataException();
            }
            if (bodySize > 512 * 1024 * 1024) {
                log.debug("in body len bad, bodySize: " + bodySize);
                throw new TooBigDataException();
            }
            // 前面head没有调整readerindex，如果header没问题，就调整buf的readerindex
            byteBuf.skipBytes(FIXED_LEN);
            bodyByteBuf = byteBuf.readRetainedSlice(bodySize);
            Object body = getObjectByByteBuf(bodyByteBuf);

            return body;
        } finally {
            if (headBuf != null) {
                headBuf.release();
            }
            if (bodyByteBuf != null) {
                bodyByteBuf.release();
            }
        }
    }
}
