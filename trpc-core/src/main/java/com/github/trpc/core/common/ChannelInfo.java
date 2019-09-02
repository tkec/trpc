package com.github.trpc.core.common;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Getter
@Slf4j
public class ChannelInfo {
    private static final AttributeKey<ChannelInfo> CLIENT_CHANNEL_KEY = AttributeKey.valueOf("client_key");
    private static final AttributeKey<ChannelInfo> SERVER_CHANNEL_KEY = AttributeKey.valueOf("server_key");

    private DynamicCompositeByteBuf recvBuf = new DynamicCompositeByteBuf(16);

    public static ChannelInfo getOrCreateClientChannelInfo(Channel channel) {
        Attribute<ChannelInfo> attribute = channel.attr(ChannelInfo.CLIENT_CHANNEL_KEY);
        ChannelInfo channelInfo = attribute.get();
        if (channelInfo == null) {
            channelInfo = new ChannelInfo();
            attribute.set(channelInfo);
        }
        return channelInfo;
    }

    public static ChannelInfo getClientChannelInfo(Channel channel) {
        Attribute<ChannelInfo> attribute = channel.attr(ChannelInfo.CLIENT_CHANNEL_KEY);
        return attribute.get();
    }

    public static ChannelInfo getOrCreateServerChannelInfo(Channel channel) {
        Attribute<ChannelInfo> attribute = channel.attr(ChannelInfo.SERVER_CHANNEL_KEY);
        ChannelInfo channelInfo = attribute.get();
        if (channelInfo == null) {
            channelInfo = new ChannelInfo();
            attribute.set(channelInfo);
        }
        return channelInfo;
    }

    public static ChannelInfo getServerChannelInfo(Channel channel) {
        Attribute<ChannelInfo> attribute = channel.attr(ChannelInfo.SERVER_CHANNEL_KEY);
        return  attribute.get();
    }
}
