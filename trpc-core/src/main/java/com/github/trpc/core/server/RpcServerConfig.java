package com.github.trpc.core.server;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RpcServerConfig {
    private Integer backlog = 1024;

    private Integer receiveBufferSize = 64 *1024;

    private Integer sendBufferSize = 64 *1024;

    private Integer acceptorThreadNum = 1;

    private Integer ioThreadNum = Runtime.getRuntime().availableProcessors();

    private Integer workThreadNum = Runtime.getRuntime().availableProcessors();

    private String protocolType;

    private String registryUrl;

    public RpcServerConfig(RpcServerConfig config) {
        this.copyFrom(config);
    }

    public void copyFrom(RpcServerConfig config) {
        this.backlog = config.backlog;
        this.receiveBufferSize = config.receiveBufferSize;
        this.sendBufferSize = config.sendBufferSize;
        this.acceptorThreadNum = config.acceptorThreadNum;
        this.ioThreadNum = config.ioThreadNum;
        this.workThreadNum = config.workThreadNum;
        this.protocolType = config.protocolType;
        this.registryUrl = config.registryUrl;
    }

}
