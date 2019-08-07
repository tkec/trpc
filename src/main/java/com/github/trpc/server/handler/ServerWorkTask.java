package com.github.trpc.server.handler;

import com.github.trpc.common.exception.RpcException;
import com.github.trpc.common.protocol.Request;
import com.github.trpc.common.protocol.Response;
import com.github.trpc.server.RpcServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 * 实际方法执行任务
 */
@Slf4j
@AllArgsConstructor
public class ServerWorkTask implements Runnable {
    private RpcServer rpcServer;
    private Request request;
    private Response response;
    private ChannelHandlerContext ctx;

    @Override
    public void run() {
        if (request == null || response == null || rpcServer == null || ctx == null) {
            return;
        }

        response.setId(request.getId());
        // invoke method
        Object target = request.getRpcMethodInfo().getTarget();
        Object[] args = request.getArgs();
        try {
            Object result = request.getRpcMethodInfo().getMethod().invoke(target, args);
            response.setResult(result);
        } catch (InvocationTargetException e) {
            Throwable targetThrowable = e.getTargetException();
            if (targetThrowable == null) {
                targetThrowable = e;
            }
            String errMsg = String.format("invoke method fail, msg=%s", e.getLocalizedMessage());
            RpcException rpcException = new RpcException(RpcException.SERVICE_EXCEPTION, errMsg);
            response.setException(rpcException);
        } catch (Throwable e2) {
            String errMsg = String.format("invoke method fail, msg=%s", e2.getLocalizedMessage());
            RpcException rpcException = new RpcException(RpcException.SERVICE_EXCEPTION, errMsg);
            response.setException(rpcException);
        }

        // send response
        try {
            ByteBuf byteBuf = rpcServer.getProtocol().encodeResponse(response);
            ctx.channel().writeAndFlush(byteBuf);
            log.debug("send result to client success.");
        } catch (Exception e) {
            log.error("send response error", e);
        }
    }
}
