package com.github.trpc.core.client.instance;

import com.github.trpc.core.client.channel.RpcChannel;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public interface InstanceProcessor {
    void addInstance(ServiceInstance serviceInstance);

    void addInstances(Collection<ServiceInstance> addList);

    void deleteInstances(Collection<ServiceInstance> deleteList);

    CopyOnWriteArrayList<ServiceInstance> getInstances();

    CopyOnWriteArrayList<RpcChannel> getRpcChannels();

    void stop();
}
