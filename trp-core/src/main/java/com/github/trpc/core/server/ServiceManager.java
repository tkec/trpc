package com.github.trpc.core.server;


import com.github.trpc.core.common.RpcMethodInfo;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceManager {

    private static volatile ServiceManager instance;

    private Map<String, RpcMethodInfo> methodInfoMap;

    private ServiceManager() {
        this.methodInfoMap = new ConcurrentHashMap<>();
    }

    public static ServiceManager getInstance() {
        if (instance == null) {
            synchronized (ServiceManager.class) {
                if (instance == null) {
                    instance = new ServiceManager();
                }
            }
        }
        return instance;
    }

    public RpcMethodInfo getService(String serviceName, String methodName) {
        return methodInfoMap.get(buildServiceKey(serviceName, methodName));
    }

    public RpcMethodInfo getService(String serviceMethodName) {
        return methodInfoMap.get(serviceMethodName);
    }

    public void registerService(Object service) {
        Class[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length != 1) {
            throw new RuntimeException("service must implement one insterface only.");
        }
        Class clazz = interfaces[0];
        Method[] methods = clazz.getDeclaredMethods();
        for(Method method : methods) {
            RpcMethodInfo rpcMethodInfo = new RpcMethodInfo(method, service);
            String key = buildServiceKey(rpcMethodInfo.getServiceName(), rpcMethodInfo.getMethodName());
            methodInfoMap.put(key, rpcMethodInfo);
        }
    }

    private String buildServiceKey(String serviceName, String methodName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(serviceName.toLowerCase()).append(".").append(methodName.toLowerCase());
        return stringBuilder.toString();
    }
}
