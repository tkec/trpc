package com.github.trpc.core.client.loadbalance;

public interface LoadBalanceFactory {
    Integer getLoadBalanceType();

    LoadBalanceStrategy createLoadBalance();
}
