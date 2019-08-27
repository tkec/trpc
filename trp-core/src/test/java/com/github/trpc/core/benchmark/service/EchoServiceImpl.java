package com.github.trpc.core.benchmark.service;

import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.core.test.service.User;
import lombok.extern.slf4j.Slf4j;

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
