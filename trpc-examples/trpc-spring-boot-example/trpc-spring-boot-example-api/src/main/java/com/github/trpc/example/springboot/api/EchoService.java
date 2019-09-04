package com.github.trpc.example.springboot.api;

import com.github.trpc.core.common.exception.RpcException;

public interface EchoService {
    String echo(String msg);

    String exception(String msg) throws RpcException;

    User getUser(String userName, Integer age);
}
