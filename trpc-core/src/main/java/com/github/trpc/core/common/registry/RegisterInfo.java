package com.github.trpc.core.common.registry;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterInfo {
    private String host;
    private Integer port;
    private String interfaceName;
}
