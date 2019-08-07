package com.github.trpc.client;

import com.github.trpc.common.exception.RpcException;
import com.github.trpc.common.protocol.Response;
import io.netty.util.Timeout;
import lombok.Setter;

import java.util.concurrent.*;

@Setter
public class RpcFuture<T> implements Future<T> {

    private CountDownLatch countDownLatch;
    private Timeout timeout;
    private Response response;
    private Boolean isDone;

    public RpcFuture() {
        countDownLatch = new CountDownLatch(1);
        isDone = false;
    }

    public void handleResponse(Response response) {
        this.response = response;
        timeout.cancel();
        countDownLatch.countDown();
        isDone = true;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public T get() throws InterruptedException {
        countDownLatch.await();
        if (response == null) {
            throw  new RpcException(RpcException.TIMEOUT_EXCEPTION);
        }
        if (response.getException() != null) {
            throw new RpcException(response.getException());
        }
        return (T)response.getResult();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException {
        countDownLatch.await(timeout, unit);
        if (response == null) {
            throw  new RpcException(RpcException.TIMEOUT_EXCEPTION);
        }
        if (response.getException() != null) {
            throw new RpcException(response.getException());
        }
        return (T)response.getResult();
    }
}
