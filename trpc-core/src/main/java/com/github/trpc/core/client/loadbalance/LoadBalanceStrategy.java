package com.github.trpc.core.client.loadbalance;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.channel.RpcChannel;
import com.github.trpc.core.common.protocol.Request;

import java.util.List;
import java.util.Set;

public interface LoadBalanceStrategy {
    Integer LOAD_BALANCE_RANDOM = 0;
    Integer LOAD_BALANCE_ROUND_ROBIN = 1;

    void init(RpcClient rpcClient);

    void destory();

    RpcChannel selectInstance(Request request,
                              List<RpcChannel> instances,
                              Set<RpcChannel> selectedInstances);
}
