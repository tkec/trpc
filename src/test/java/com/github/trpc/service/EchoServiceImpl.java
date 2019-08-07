package com.github.trpc.service;

import com.github.trpc.common.exception.RpcException;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class EchoServiceImpl implements EchoService {
    @Override
    public String echo(String msg) {
        String result = "EchoService from Server: " + msg + ", " + new Date();
        log.info("server send result:" + result);
        return result;
    }

    @Override
    public String exception(String msg) throws RpcException {
        throw new RpcException(RpcException.SERVICE_EXCEPTION, "exception in server");
    }

    @Override
    public User getUser(String userName, Integer age) {
        log.info("receive username="+userName+", age=" + age);
        return new User(userName, age);
    }
}
