package com.github.trpc.core.common.registry;

import com.github.trpc.core.client.instance.ServiceInstance;

import java.util.Collection;

public interface NotifyListener {

    void notify(Collection<ServiceInstance> addList, Collection<ServiceInstance> deleteList);
}
