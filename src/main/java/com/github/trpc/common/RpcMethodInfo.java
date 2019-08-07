package com.github.trpc.common;

import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

@Getter
public class RpcMethodInfo {
    private Method method;
    private String serviceName;
    private String methodName;
    private Type[] inputClasses;
    private Type outputClass;
    private Object target;

    public RpcMethodInfo(Method method) {
        this.method = method;
        this.serviceName = method.getDeclaringClass().getName();
        this.methodName = method.getName();
        Type[] inputClasses = method.getGenericParameterTypes();
        if (inputClasses.length < 1) {
            throw new IllegalArgumentException("invalid method");
        }
        this.inputClasses = inputClasses;
        this.outputClass = method.getGenericReturnType();
    }

    public RpcMethodInfo(Method method, Object target) {
        this(method);
        this.target = target;
    }

}
