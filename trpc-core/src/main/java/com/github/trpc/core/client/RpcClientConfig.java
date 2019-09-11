package com.github.trpc.core.client;

import com.github.trpc.core.client.loadbalance.LoadBalanceStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RpcClientConfig {
    private Integer connectTimeoutMillis = 1000;

    private Integer readTimeoutMillis = 1000;

    private Integer writeTimeoutMillis = 1000;

    private Integer backlog = 1024;

    private Integer receiveBufferSize = 64 *1024;

    private Integer sendBufferSize = 64 *1024;

    private Integer acceptorThreadNum = 1;

    private Integer ioThreadNum = Runtime.getRuntime().availableProcessors();

    private Integer workThreadNum = Runtime.getRuntime().availableProcessors();

    private String protocolType;

    private String registryUrl;

    private Integer loadBalanceType = LoadBalanceStrategy.LOAD_BALANCE_ROUND_ROBIN;

    public RpcClientConfig(RpcClientConfig config) {
        this.copyFrom(config);
    }

    public void copyFrom(RpcClientConfig config) {
        this.connectTimeoutMillis = config.connectTimeoutMillis;
        this.readTimeoutMillis = config.readTimeoutMillis;
        this.writeTimeoutMillis = config.writeTimeoutMillis;
        this.backlog = config.backlog;
        this.receiveBufferSize = config.receiveBufferSize;
        this.sendBufferSize = config.sendBufferSize;
        this.acceptorThreadNum = config.acceptorThreadNum;
        this.ioThreadNum = config.ioThreadNum;
        this.workThreadNum = config.workThreadNum;
        this.protocolType = config.protocolType;
        this.registryUrl = config.registryUrl;
        this.loadBalanceType = config.loadBalanceType;
    }
}
