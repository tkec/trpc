package com.github.trpc.common.protocol;

import java.io.Serializable;

public interface Request extends Serializable {

    long getId();

    void setId(long id);

    String getServiceName();

    void setServiceName(String serviceName);

    String getMethodName();

    void setMethodName(String methodName);

    Object[] getArgs();

    void setArgs(Object[] args);
}
