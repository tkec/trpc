package com.github.trpc.springboot.annotation;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RpcClientConfig {
    private String serviceUrl;
}
