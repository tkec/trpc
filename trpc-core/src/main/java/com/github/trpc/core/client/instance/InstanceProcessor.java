package com.github.trpc.core.client.instance;

import com.github.trpc.core.client.channel.RpcChannel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface InstanceProcessor {
    void addInstance(ServiceInstance serviceInstance);

    void addInstances(List<ServiceInstance> addList);

    void deleteInstances(List<ServiceInstance> deleteList);

    CopyOnWriteArrayList<ServiceInstance> getInstances();

    CopyOnWriteArrayList<RpcChannel> getRpcChannels();

    void stop();
}
