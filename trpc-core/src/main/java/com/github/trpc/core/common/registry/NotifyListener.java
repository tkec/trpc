package com.github.trpc.core.common.registry;

import com.github.trpc.core.client.instance.ServiceInstance;

import java.util.List;

public interface NotifyListener {

    void notify(List<ServiceInstance> addList, List<ServiceInstance> deleteList);
}
