package com.github.trpc.client.handler;

import com.github.trpc.client.RpcClient;
import com.github.trpc.common.ChannelInfo;
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
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelInfo channelInfo = ChannelInfo.getOrCreateClientChannelInfo(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("receive from server.");
        try {
            ChannelInfo channelInfo = ChannelInfo.getClientChannelInfo(ctx.channel());
            ByteBuf in = (ByteBuf) msg;
            if (in.readableBytes() > 0) {
                channelInfo.getRecvBuf().addBuffer(in.retain());
            }
            while (channelInfo.getRecvBuf().readableBytes() > 0) {
                log.debug("recv buf readindex:" + channelInfo.getRecvBuf().readableBytes());
                try {
                    Response response = rpcClient.getProtocol().decodeResponse(channelInfo.getRecvBuf());
                    log.debug("decode response in handler:" + response.getId());
                    rpcClient.getWorkThreadPool().submit(new ClientWorkTask(rpcClient, response));
                } catch (NotEnoughDataException e1) {
                    break;
                } catch (TooBigDataException e2) {
                    e2.printStackTrace();
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e2);
                } catch (BadSchemaException e3) {
                    e3.printStackTrace();
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e3);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e);
        }
    }


}
