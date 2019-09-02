package com.github.trpc.spring.annotation;

import com.github.trpc.core.client.Endpoint;
import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.RpcProxy;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class RpcClientFactoryBean implements FactoryBean, InitializingBean, DisposableBean {

    private Class serviceInterface;

    private Endpoint endpoint;

    private Object serviceProxy;

    private RpcClient rpcClient;

    public void setServiceInterface(Class serviceInterface) {
        if (serviceInterface != null && !serviceInterface.isInterface()) {
            throw new IllegalArgumentException("'serviceInterface' must be an interface");
        }
        this.serviceInterface = serviceInterface;
    }


    @Override
    public void destroy() throws Exception {
        if (rpcClient != null) {
            rpcClient.shutdown();
        }
    }

    @Override
    public Object getObject() throws Exception {
        return serviceProxy;
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (rpcClient == null) {
            rpcClient = new RpcClient(endpoint);
        }
        this.serviceProxy = RpcProxy.getProxy(rpcClient, serviceInterface);
    }
}
