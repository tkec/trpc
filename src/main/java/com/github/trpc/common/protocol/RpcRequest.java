package com.github.trpc.common.protocol;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RpcRequest implements Request {
    private long id;
    private String serviceName;
    private String methodName;
    private Object[] args;
}
