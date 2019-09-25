package com.github.trpc.core.common.registry;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SubscribeInfo {
    private String interfaceName;
    private RegistryConfig registryConfig;
}
