package com.github.trpc.core.common.extension;

import com.github.trpc.core.client.loadbalance.LoadBalanceFactory;
import com.github.trpc.core.client.loadbalance.LoadBalanceManager;
import com.github.trpc.core.common.registry.RegistryFactory;
import com.github.trpc.core.common.registry.RegistryFactoryManager;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExtensionLoaderManager {

    private static volatile ExtensionLoaderManager instance;

    private AtomicBoolean load = new AtomicBoolean(false);

    private ExtensionLoaderManager() {

    }

    public static ExtensionLoaderManager getInstance() {
        if (instance == null) {
            synchronized (ExtensionLoaderManager.class) {
                if (instance == null) {
                    instance = new ExtensionLoaderManager();
                }
            }
        }
        return instance;
    }

    public void loadAllExtensions() {
        if (load.compareAndSet(false, true)) {
            loadRegistry();
            loadLoadBalance();
        }
    }

    private void loadRegistry() {
        RegistryFactoryManager manager = RegistryFactoryManager.getInstance();
        ServiceLoader<RegistryFactory> registryFactories = ServiceLoader.load(RegistryFactory.class);
        for (RegistryFactory registryFactory : registryFactories) {
            manager.registerRegistryFactory(registryFactory);
        }
    }

    private void loadLoadBalance() {
        LoadBalanceManager manager = LoadBalanceManager.getInstance();
        ServiceLoader<LoadBalanceFactory> loadBalanceFactories = ServiceLoader.load(LoadBalanceFactory.class);
        for (LoadBalanceFactory loadBalanceFactory : loadBalanceFactories) {
            manager.registerLoadBalanceFactory(loadBalanceFactory);
        }
    }
}
