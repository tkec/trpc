package com.github.trpc.core.common.registry.list;

import com.github.trpc.core.common.registry.Registry;
import com.github.trpc.core.common.registry.RegistryFactory;
import com.github.trpc.core.common.registry.RpcURL;

public class ListRegistryFactory implements RegistryFactory {

    public static final String NAME = "list";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Registry createRegistry(RpcURL url) {
        String schema = url.getSchema();
        if (schema.equals(NAME)) {
            return new ListRegistry(url);
        } else {
            throw new IllegalArgumentException("schema is invalid:" + schema);
        }
    }
}
