package com.github.trpc.springboot.registry;

import com.github.trpc.core.common.registry.Registry;
import com.github.trpc.core.common.registry.RegistryFactory;
import com.github.trpc.core.common.registry.RpcURL;

public class SpringCloudRegistryFactory implements RegistryFactory {

    public static final String NAME = "springcloud";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Registry createRegistry(RpcURL url) {
        String schema = url.getSchema();
        if (schema.equals(NAME)) {
            return new SpringCloudRegistry(url);
        } else {
            throw new IllegalArgumentException("schema is invalid:" + schema);
        }
    }
}
