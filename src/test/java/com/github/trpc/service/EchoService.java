package com.github.trpc.service;

import com.github.trpc.common.exception.RpcException;

public interface EchoService {
    String echo(String msg);

    String exception(String msg) throws RpcException;

    User getUser(String userName, Integer age);
}
