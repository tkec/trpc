package com.github.trpc.core.common.registry.list;

import com.github.trpc.core.common.registry.Registry;
import com.github.trpc.core.common.registry.RegistryFactory;
import com.github.trpc.core.common.registry.RpcURL;

public class ListRegistryFactory implements RegistryFactory {
    @Override
    public String getName() {
        return "list";
    }

    @Override
    public Registry createRegistry(RpcURL url) {
        String schema = url.getSchema();
        if (schema.equals("list")) {
            return new ListRegistry(url);
        } else {
            throw new IllegalArgumentException("schema is invalid:" + schema);
        }
    }
}
