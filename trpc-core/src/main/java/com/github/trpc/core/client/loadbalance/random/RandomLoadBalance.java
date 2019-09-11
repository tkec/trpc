package com.github.trpc.core.client.loadbalance.random;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.channel.RpcChannel;
import com.github.trpc.core.client.loadbalance.LoadBalanceStrategy;
import com.github.trpc.core.common.protocol.Request;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomLoadBalance implements LoadBalanceStrategy {

    private Random random = new Random();

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
        Collection<RpcChannel> toBeSelectedInstances = null;
        if (selectedInstances == null) {
            toBeSelectedInstances = instances;
        } else {
            toBeSelectedInstances = CollectionUtils.subtract(instances, selectedInstances);
        }

        Integer instanceNum = toBeSelectedInstances.size();
        if (instanceNum == 0) {
            toBeSelectedInstances = instances;
            instanceNum = toBeSelectedInstances.size();
        }

        if (instanceNum == 0) {
            return null;
        }

        Integer index = getRandomInt(instanceNum);
        RpcChannel rpcChannel = toBeSelectedInstances.toArray(new RpcChannel[0])[index];
        return rpcChannel;
    }

    private Integer getRandomInt(int bound) {
        return random.nextInt(bound);
    }
}
