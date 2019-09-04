package com.github.trpc.example.springboot.server;

import com.github.trpc.core.common.exception.RpcException;
import com.github.trpc.example.springboot.api.EchoService;
import com.github.trpc.example.springboot.api.User;
import com.github.trpc.springboot.annotation.TRpcService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TRpcService
public class EchoSeviceImpl implements EchoService {
    @Override
    public String echo(String msg) {
        log.info("echo msg:" + msg);
        return "spring boot server msg:" + msg;
    }

    @Override
    public String exception(String msg) throws RpcException {
        log.info("exception msg:" + msg);
        throw new RpcException("spring boot server exception msg:" + msg);
    }

    @Override
    public User getUser(String userName, Integer age) {
        log.info("username:" + userName + ", age:" +age);
        return new User(userName, age);
    }
}
