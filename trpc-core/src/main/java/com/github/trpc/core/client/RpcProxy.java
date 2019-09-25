package com.github.trpc.core.client;

import com.github.trpc.core.common.RpcMethodInfo;
import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.common.protocol.Request;
import com.github.trpc.core.common.protocol.Response;
import com.github.trpc.core.common.registry.RegistryConfig;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcProxy implements MethodInterceptor {

    private RpcClient rpcClient;
    private Map<String, RpcMethodInfo> rpcMethodInfoMap = new ConcurrentHashMap<>();
    private static final Set<String> notProxyMethodSet = new HashSet<>();

    static {
        notProxyMethodSet.add("getClass");
        notProxyMethodSet.add("hashCode");
        notProxyMethodSet.add("equals");
        notProxyMethodSet.add("clone");
        notProxyMethodSet.add("toString");
        notProxyMethodSet.add("notify");
        notProxyMethodSet.add("notifyAll");
        notProxyMethodSet.add("wait");
        notProxyMethodSet.add("finalize");
    }

    public static <T> T getProxy(RpcClient rpcClient, Class clazz) {
        return getProxy(rpcClient, clazz, null);
    }

    public static <T> T getProxy(RpcClient rpcClient, Class clazz, RegistryConfig registryConfig) {
        rpcClient.addServiceInterface(clazz, registryConfig);
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(new RpcProxy(rpcClient, clazz));
        return (T) enhancer.create();
    }


    /**
     * 调用接口时，实际执行的方法
     * @param target
     * @param method
     * @param args
     * @param methodProxy
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        String methodName = method.getName();
        RpcMethodInfo rpcMethodInfo = rpcMethodInfoMap.get(methodName);
        if (rpcMethodInfo == null) {
            log.debug("{}:{} does not need to proxy",
                    method.getDeclaringClass().getName(), method.getName());
            return methodProxy.invokeSuper(target, args);
        }

        try {
            Request request = rpcClient.getProtocol().createRequest();
//            request.setTarget(target);
//            request.setRpcMethodInfo(rpcMethodInfo);
            request.setServiceName(rpcMethodInfo.getServiceName());
            request.setMethodName(rpcMethodInfo.getMethodName());
            request.setArgs(args);

            Response response = rpcClient.getProtocol().createResponse();
            int maxRetryTimes = 3;
            int currentTryTime = 1;
            while (currentTryTime <= maxRetryTimes) {
                try {
                    invokeRpc(request, response);
                    log.debug(currentTryTime + " call id=" + request.getId());
                    break;
                } catch (Exception e) {
                    log.error("send request error, id=" + request.getId() + ", e=" + e.getMessage());
                } finally {
                    currentTryTime++;
                }
            }

            if (response.getResult() == null) {
                if (response.getException() != null) {
                    throw new RpcException(response.getException());
                } else {
                    throw new RpcException("unknown exception");
                }
            }
            return response.getResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RpcException(e.getMessage());
        }
    }

    protected void invokeRpc(Request request, Response response) throws Exception {
        Channel channel = rpcClient.selectChannel(request);
        rpcCore(request, channel, response);
    }

    protected void rpcCore(Request request, Channel channel, Response response) throws Exception {
        try {
            RpcFuture rpcFuture = rpcClient.sendRequest(request, channel);
            response.setResult(rpcFuture.get(20000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.error("exception in rpc request, id=" + request.getId() + ", e=" + e.getMessage());
            throw e;
        }
    }

    protected RpcProxy(RpcClient rpcClient, Class clazz) {
        this.rpcClient = rpcClient;
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (notProxyMethodSet.contains(method.getName())) {
                log.debug("{}:{} does not need to proxy",
                        method.getDeclaringClass().getName(), method.getName());
                continue;
            }
            rpcMethodInfoMap.put(method.getName(), new RpcMethodInfo(method));
        }
    }
}
