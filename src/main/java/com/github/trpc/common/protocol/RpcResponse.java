package com.github.trpc.common.protocol;

import com.github.trpc.common.protocol.Response;
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
