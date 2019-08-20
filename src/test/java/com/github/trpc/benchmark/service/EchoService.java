package com.github.trpc.benchmark.service;

import com.github.trpc.common.exception.RpcException;
import com.github.trpc.test.service.User;

public interface EchoService {
    String echo(String msg);

    String exception(String msg) throws RpcException;

    User getUser(String userName, Integer age);
}
