package com.github.trpc.server.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcServerChannelIdleHandler extends ChannelDuplexHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RpcServerChannelIdleHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) event;
            if (e.state() == IdleState.ALL_IDLE) {
                // if no read and write for period time, close current channel
                LOG.debug("channel={} ip={} is idle for period time, close now.",
                        ctx.channel(), ctx.channel().remoteAddress());
                ctx.close();
            } else {
                LOG.debug("idle on channel[{}]:{}", e.state(), ctx.channel());
            }
        }
    }

}