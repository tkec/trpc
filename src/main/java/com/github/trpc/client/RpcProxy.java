package com.github.trpc.client;

import com.github.trpc.common.RpcMethodInfo;
import com.github.trpc.common.exception.RpcException;
import com.github.trpc.common.protocol.Request;
import com.github.trpc.common.protocol.Response;
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
        rpcClient.setServiceInterface(clazz);
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
            request.setTarget(target);
            request.setRpcMethodInfo(rpcMethodInfo);
            request.setServiceName(rpcMethodInfo.getServiceName());
            request.setMethodName(rpcMethodInfo.getMethodName());
            request.setArgs(args);

            Response response = rpcClient.getProtocol().createResponse();
            invokeRpc(request, response);
            if (response.getException() != null) {
                throw new RpcException(response.getException());
            }
            return response.getResult();
        } catch (Exception e) {
            throw new RpcException(e.getMessage());
        }
    }

    protected void invokeRpc(Request request, Response response) throws Exception {
        Channel channel = rpcClient.selectChannel(request);
        rpcCore(request, channel, response);
    }

    protected void rpcCore(Request request, Channel channel, Response response) throws Exception {
        RpcFuture rpcFuture = rpcClient.sendRequest(request, channel);
        response.setResult(rpcFuture.get(1000, TimeUnit.MILLISECONDS));
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
