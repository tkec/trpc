package com.github.trpc.core.client.loadbalance.random;

import com.github.trpc.core.client.loadbalance.LoadBalanceFactory;
import com.github.trpc.core.client.loadbalance.LoadBalanceStrategy;

public class RandomLoadBalanceFactory implements LoadBalanceFactory {
    @Override
    public Integer getLoadBalanceType() {
        return LoadBalanceStrategy.LOAD_BALANCE_RANDOM;
    }

    @Override
    public LoadBalanceStrategy createLoadBalance() {
        return new RandomLoadBalance();
    }
}
