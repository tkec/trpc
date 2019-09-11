package com.github.trpc.core.common.registry;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RegistryFactoryManager {
    private static volatile RegistryFactoryManager instance;

    private Map<String, RegistryFactory> registryFactoryMap;

    private RegistryFactoryManager() {
        registryFactoryMap = new HashMap<>();
    }

    public static RegistryFactoryManager getInstance() {
        if (instance == null) {
            synchronized (RegistryFactoryManager.class) {
                if (instance == null) {
                    instance = new RegistryFactoryManager();
                }
            }
        }
        return instance;
    }

    public void registerRegistryFactory(RegistryFactory registryFactory) {
        if (registryFactoryMap.get(registryFactory.getName()) != null) {
            throw new RuntimeException("RegistryFactory exist: " + registryFactory.getName());
        }
        registryFactoryMap.put(registryFactory.getName(), registryFactory);
        log.info("register Registry factory success, factory: " + registryFactory);
    }

    public RegistryFactory getRegistryFactory(String name) {
        return registryFactoryMap.get(name);
    }
}
