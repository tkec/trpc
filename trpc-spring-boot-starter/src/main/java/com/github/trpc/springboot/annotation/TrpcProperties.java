package com.github.trpc.springboot.annotation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "trpc")
public class TrpcProperties {
    private RpcServerConfig server;
    private RpcClientConfig client;
}


