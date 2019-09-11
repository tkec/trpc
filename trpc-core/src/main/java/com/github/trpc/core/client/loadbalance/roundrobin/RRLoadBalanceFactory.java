package com.github.trpc.core.client.loadbalance.roundrobin;

import com.github.trpc.core.client.loadbalance.LoadBalanceFactory;
import com.github.trpc.core.client.loadbalance.LoadBalanceStrategy;

public class RRLoadBalanceFactory implements LoadBalanceFactory {
    @Override
    public Integer getLoadBalanceType() {
        return LoadBalanceStrategy.LOAD_BALANCE_ROUND_ROBIN;
    }

    @Override
    public LoadBalanceStrategy createLoadBalance() {
        return new RRLoadBalance();
    }
}
