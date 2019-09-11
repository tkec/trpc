package com.github.trpc.core.client.instance;

import com.github.trpc.core.client.RpcClient;
import com.github.trpc.core.client.channel.RpcChannel;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BasicInstanceProcessor implements InstanceProcessor {

    private CopyOnWriteArrayList<ServiceInstance> instances;
    private CopyOnWriteArrayList<RpcChannel> channels;
    private ConcurrentHashMap<ServiceInstance, RpcChannel> instanceChannelMap;
    private Lock lock;
    private RpcClient rpcClient;

    public BasicInstanceProcessor(RpcClient rpcClient) {
        this.instances = new CopyOnWriteArrayList<>();
        this.channels = new CopyOnWriteArrayList<>();
        this.instanceChannelMap = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.rpcClient = rpcClient;
    }


    @Override
    public void addInstance(ServiceInstance serviceInstance) {
        lock.lock();
        try {
            if (instances.add(serviceInstance)) {
                RpcChannel rpcChannel = new RpcChannel(serviceInstance, rpcClient);
                channels.add(rpcChannel);
                instanceChannelMap.put(serviceInstance, rpcChannel);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addInstances(List<ServiceInstance> addList) {
        for (ServiceInstance serviceInstance : addList) {
            addInstance(serviceInstance);
        }
    }

    @Override
    public void deleteInstances(List<ServiceInstance> deleteList) {
        for (ServiceInstance serviceInstance : deleteList) {
            deleteInstance(serviceInstance);
        }
    }

    private void deleteInstance(ServiceInstance instance) {
        lock.lock();
        try {
            if (instances.remove(instance)) {
                Iterator<RpcChannel> iterator = channels.iterator();
                while (iterator.hasNext()) {
                    RpcChannel rpcChannel = iterator.next();
                    if (rpcChannel.getServiceInstance().equals(instance)) {
                        rpcChannel.close();
                        channels.remove(rpcChannel);
                        instanceChannelMap.remove(instance);
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CopyOnWriteArrayList<ServiceInstance> getInstances() {
        return instances;
    }

    @Override
    public CopyOnWriteArrayList<RpcChannel> getRpcChannels() {
        return channels;
    }

    @Override
    public void stop() {
        for (RpcChannel rpcChannel : channels) {
            rpcChannel.close();
        }
    }
}
