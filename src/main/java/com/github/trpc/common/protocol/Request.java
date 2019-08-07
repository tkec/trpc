package com.github.trpc.common.protocol;

import com.github.trpc.common.RpcMethodInfo;

import java.lang.reflect.Method;

public interface Request {
    Object getMsg();

    void setMsg(Object object);

    long getId();

    void setId(long id);

    Object getTarget();

    void setTarget(Object target);

    Method getTargetMethod();

    void setTargetMethod(Method method);

    String getServiceName();

    void setServiceName(String serviceName);

    String getMethodName();

    void setMethodName(String methodName);

    Object[] getArgs();

    void setArgs(Object[] args);

    RpcMethodInfo getRpcMethodInfo();

    void setRpcMethodInfo(RpcMethodInfo rpcMethodInfo);

    Exception getException();

    void setException(Exception e);
}
