package com.github.trpc.core.common.registry;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RpcURL {

    private String schema;
    private String hostPorts;

    public RpcURL(String uri) {
        if(uri == null) {
            throw new RuntimeException("uri is null");
        }
        int index = uri.indexOf("://");
        if (index < 0) {
            throw new IllegalArgumentException("invalid uri:" + uri);
        }
        schema = uri.substring(0, index).toLowerCase();
        hostPorts = uri.substring(index + 3);
    }
}
