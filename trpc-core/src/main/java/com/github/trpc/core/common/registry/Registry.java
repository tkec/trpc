package com.github.trpc.core.common.registry;

import com.github.trpc.core.client.instance.ServiceInstance;

import java.util.List;

public interface Registry {
    List<ServiceInstance> lookup(SubscribeInfo subscribeInfo);

    default void subscribe(SubscribeInfo subscribeInfo, NotifyListener listener) {}

    default void unSubscribe(SubscribeInfo subscribeInfo) {}

    default void register(RegisterInfo registerInfo) {}

    default void unRegister(RegisterInfo registerInfo) {}
}
