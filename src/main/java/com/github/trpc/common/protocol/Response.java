package com.github.trpc.common.protocol;

public interface Response {
    Object getResult();

    void setResult(Object result);

    long getId();

    void setId(long id);

    Exception getException();

    void setException(Exception e);
}
