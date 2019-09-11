package com.github.trpc.core.common.registry.list;

import com.github.trpc.core.client.instance.ServiceInstance;
import com.github.trpc.core.common.registry.Registry;
import com.github.trpc.core.common.registry.RpcURL;
import com.github.trpc.core.common.registry.SubscribeInfo;

import java.util.ArrayList;
import java.util.List;

public class ListRegistry implements Registry {
    private List<ServiceInstance> instances;

    public ListRegistry(RpcURL url) {
        if (url == null) {
            throw new IllegalArgumentException("url is null");
        }
        String hostPorts = url.getHostPorts();
        String[] hostPortSplits = hostPorts.split(",");
        this.instances = new ArrayList<>(hostPortSplits.length);
        for (String hostPort : hostPortSplits) {
            String[] hostPortSplit = hostPort.split(":");
            String host = hostPortSplit[0];
            int port;
            if (hostPortSplit.length == 2) {
                port = Integer.valueOf(hostPortSplit[1]);
            } else {
                port = 80;
            }
            instances.add(new ServiceInstance(host, port));
        }
    }

    @Override
    public List<ServiceInstance> lookup(SubscribeInfo subscribeInfo) {
        return instances;
    }
}
