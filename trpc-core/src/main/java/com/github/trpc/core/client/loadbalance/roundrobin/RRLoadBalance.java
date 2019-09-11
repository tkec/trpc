package com.github.trpc.core.client.loadbalance.roundrobin;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.channel.RpcChannel;
import com.github.trpc.core.client.loadbalance.LoadBalanceStrategy;
import com.github.trpc.core.common.protocol.Request;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class RRLoadBalance implements LoadBalanceStrategy {

    private AtomicLong counter = new AtomicLong();

    @Override
    public void init(RpcClient rpcClient) {

    }

    @Override
    public void destory() {

    }

    @Override
    public RpcChannel selectInstance(Request request, List<RpcChannel> instances, Set<RpcChannel> selectedInstances) {
        if (instances == null || instances.size() == 0) {
            return null;
        }

        Integer instanceNum = instances.size();

        Integer index = (int) (counter.getAndIncrement() % instanceNum);
        RpcChannel rpcChannel = instances.get(index);
        return rpcChannel;
    }
}
