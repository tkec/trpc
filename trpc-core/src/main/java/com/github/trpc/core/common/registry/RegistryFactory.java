package com.github.trpc.core.common.registry;

public interface RegistryFactory {
    String getName();

    Registry createRegistry(RpcURL url);
}
