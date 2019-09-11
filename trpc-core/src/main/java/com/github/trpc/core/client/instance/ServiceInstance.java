package com.github.trpc.core.client.instance;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode
public class ServiceInstance extends Endpoint {
    public ServiceInstance() {
        super();
    }

    public ServiceInstance(String ip, int port) {
        super(ip, port);
    }

    public ServiceInstance(Endpoint endpoint) {
        super(endpoint.getHost(), endpoint.getPort());
    }
}
