package com.github.trpc.common.protocol;

import com.github.trpc.common.RpcMethodInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Method;

@Setter
@Getter
@ToString
public class RpcRequest implements Request {
    private Object msg;
    private long id;
    private Object target;
    private Method targetMethod;
    private RpcMethodInfo rpcMethodInfo;
    private String serviceName;
    private String methodName;
    private Object[] args;
    private Exception exception;
}
