package com.github.trpc.common.protocol;

import java.io.Serializable;

public interface Response extends Serializable {
    Object getResult();

    void setResult(Object result);

    long getId();

    void setId(long id);

    Exception getException();

    void setException(Exception e);
}
