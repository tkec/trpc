package com.github.trpc.client.handler;

import com.github.trpc.client.RpcClient;
import com.github.trpc.common.exception.BadSchemaException;
import com.github.trpc.common.exception.NotEnoughDataException;
import com.github.trpc.common.exception.RpcException;
import com.github.trpc.common.exception.TooBigDataException;
import com.github.trpc.common.protocol.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@ChannelHandler.Sharable
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<Object> {

    private RpcClient rpcClient;

    public RpcClientHandler(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("receive from server.");
        ByteBuf in = (ByteBuf)msg;
        while (in.readableBytes() > 0) {
            try {
                Response response = rpcClient.getProtocol().decodeResponse(in);
                log.debug("decode response in handler:" + response);
                rpcClient.getWorkThreadPool().submit(new ClientWorkTask(rpcClient, response));
            } catch (NotEnoughDataException e1) {
                break;
            } catch (TooBigDataException e2) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e2);
            } catch (BadSchemaException e3) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e3);
            }
        }
    }
}
