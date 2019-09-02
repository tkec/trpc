package com.github.trpc.core.benchmark.service;

import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.test.service.User;

public interface EchoService {
    String echo(String msg);

    String exception(String msg) throws RpcException;

    User getUser(String userName, Integer age);
}
