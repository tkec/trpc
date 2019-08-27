package com.github.trpc.core.common.protocol;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RpcResponse implements Response {
    private Object result;
    private long id;
    private Exception exception;
}
