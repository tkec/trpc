package com.github.trpc.springboot.registry;

import com.github.trpc.core.client.instance.ServiceInstance;
import com.github.trpc.core.common.registry.NotifyListener;
import com.github.trpc.core.common.registry.Registry;
import com.github.trpc.core.common.registry.RpcURL;
import com.github.trpc.core.common.registry.SubscribeInfo;
import com.github.trpc.core.common.thread.CustomThreadFactory;
import com.github.trpc.springboot.autoconfigure.ApplicationContextHolder;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SpringCloudRegistry implements Registry {

    private static final Integer UPDATE_INTERVAL = 5000;

    private List<ServiceInstance> lastInstances = new ArrayList<>();
    private Timer timer;
    private Integer updateInterval;
    private DiscoveryClient discoveryClient;

    public SpringCloudRegistry(RpcURL url) {
        if (url == null) {
            throw new IllegalArgumentException("url is null");
        }
        this.updateInterval = UPDATE_INTERVAL;
        timer = new HashedWheelTimer(new CustomThreadFactory("springcloud-registry-timer-thread"));
        discoveryClient = ApplicationContextHolder.getBean(DiscoveryClient.class);
        if (discoveryClient == null) {
            throw new RuntimeException("DisconveryClient is null");
        }
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        List<org.springframework.cloud.client.ServiceInstance> discoveryInstances =
                discoveryClient.getInstances(subscribeInfo.getRegistryConfig().getServiceId());
        List<ServiceInstance> instances = new ArrayList<>();
        for (org.springframework.cloud.client.ServiceInstance discoveryInstance : discoveryInstances) {
            String host = discoveryInstance.getHost();
            Integer port = Integer.valueOf(discoveryInstance.getMetadata().get(MetaDataEnvProcessor.META_PORT_KEY));
            ServiceInstance instance = new ServiceInstance(host, port);
            instances.add(instance);
        }
        return instances;
    }

    @Override
    public void subscribe(SubscribeInfo subscribeInfo, NotifyListener listener) {
        timer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                try {
                    List<ServiceInstance> currentInstances = lookup(subscribeInfo);
                    Collection<ServiceInstance> addList = CollectionUtils.subtract(currentInstances, lastInstances);
                    Collection<ServiceInstance> deleteList = CollectionUtils.subtract(lastInstances, currentInstances);
                    listener.notify(addList, deleteList);
                    lastInstances = currentInstances;
                } catch (Exception e) {
                    // ignore
                }
                timer.newTimeout(this, updateInterval, TimeUnit.MILLISECONDS);
            }
        }, updateInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void unSubscribe(SubscribeInfo subscribeInfo) {
        timer.stop();
    }
}
