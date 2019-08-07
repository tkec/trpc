package com.github.trpc.server.handler;

import com.github.trpc.common.exception.BadSchemaException;
import com.github.trpc.common.exception.NotEnoughDataException;
import com.github.trpc.common.exception.RpcException;
import com.github.trpc.common.exception.TooBigDataException;
import com.github.trpc.common.protocol.Request;
import com.github.trpc.common.protocol.Response;
import com.github.trpc.common.RpcMethodInfo;
import com.github.trpc.server.RpcServer;
import com.github.trpc.server.ServiceManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@ChannelHandler.Sharable
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<Object> {

    private RpcServer rpcServer;

    public RpcServerHandler(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channel is active, ch=" + ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channel is inactive, ch=" + ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("receive client request.");
        ByteBuf in = (ByteBuf)msg;
        while (in.readableBytes() > 0) {
            try {
                Request request = rpcServer.getProtocol().decodeRequest(in);
                Response response = rpcServer.getProtocol().createResponse();
                RpcMethodInfo rpcMethodInfo = ServiceManager.getInstance().
                        getService(request.getServiceName(), request.getMethodName());
                if (request == null || rpcMethodInfo == null) {
                    try {
                        String errMsg = String.format("fail to find service=%s, method=%s",
                                request.getServiceName(), request.getMethodName());
                        log.error(errMsg);
                        RpcException exception = new RpcException(RpcException.SERVICE_EXCEPTION, errMsg);
                        response.setException(exception);
                        ByteBuf byteBuf = rpcServer.getProtocol().encodeResponse(response);
                        ctx.channel().writeAndFlush(byteBuf);
                    } catch (Exception e) {
                        log.error("send response fail, " + e.getLocalizedMessage());
                    }
                    return;
                }
                request.setRpcMethodInfo(rpcMethodInfo);
                rpcServer.getWorkThreadPool().submit(new ServerWorkTask(rpcServer, request, response, ctx));
            } catch (NotEnoughDataException e1) {
                break;
            } catch (TooBigDataException e2) {
                e2.printStackTrace();
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e2);
            } catch (BadSchemaException e3) {
                e3.printStackTrace();
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e3);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception happened, ch=" + ctx.channel() + ",ex=" + cause.getLocalizedMessage());
        ctx.close();
    }


}
