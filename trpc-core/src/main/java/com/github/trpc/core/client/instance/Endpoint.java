package com.github.trpc.core.client.instance;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Endpoint {
    private String host;
    private Integer port;

    public Endpoint() {

    }

    public Endpoint(String host, Integer port) {
        this.host = host;
        this.port = port;
    }
}
