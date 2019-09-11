package com.github.trpc.core.client.loadbalance;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LoadBalanceManager {

    private static volatile LoadBalanceManager instance;
    private Map<Integer, LoadBalanceFactory> loadBalanceFactoryMap;

    private LoadBalanceManager() {
        loadBalanceFactoryMap = new HashMap<>();
    }

    public static LoadBalanceManager getInstance() {
        if (instance == null) {
            synchronized (LoadBalanceManager.class) {
                if (instance == null) {
                    instance = new LoadBalanceManager();
                }
            }
        }
        return instance;
    }

    public void registerLoadBalanceFactory(LoadBalanceFactory loadBalanceFactory) {
        if (loadBalanceFactoryMap.get(loadBalanceFactory.getLoadBalanceType()) != null) {
            throw new RuntimeException("load balance factory exist, type=" + loadBalanceFactory.getLoadBalanceType());
        }
        loadBalanceFactoryMap.put(loadBalanceFactory.getLoadBalanceType(), loadBalanceFactory);
        log.info("register load balance factory success, factory: " + loadBalanceFactory);
    }

    public LoadBalanceStrategy createLoadBalance(Integer loadBanalceType) {
        LoadBalanceFactory loadBalanceFactory = loadBalanceFactoryMap.get(loadBanalceType);
        if (loadBalanceFactory == null) {
            throw new RuntimeException("create load balance failed, load balance factory not found");
        }
        return loadBalanceFactory.createLoadBalance();
    }
}
