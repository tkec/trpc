package com.github.trpc.benchmark.service;

import com.github.trpc.common.exception.RpcException;
import com.github.trpc.test.service.User;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EchoServiceImpl implements EchoService {
    @Override
    public String echo(String msg) {
//        try {
//            TimeUnit.MILLISECONDS.sleep(100);
//        } catch (InterruptedException e) {
//
//        }
        return msg;
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
