package com.github.trpc.springboot.annotation;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.RpcProxy;
import com.github.trpc.core.common.registry.RegistryConfig;
import lombok.Setter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

@Setter
public class RpcClientFactoryBean implements FactoryBean, InitializingBean, DisposableBean {

    private Class serviceInterface;

    private String serviceUrl;

    private String serviceId;

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
            rpcClient = new RpcClient(serviceUrl);
        }
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setServiceId(serviceId);
        this.serviceProxy = RpcProxy.getProxy(rpcClient, serviceInterface, registryConfig);
    }
}
