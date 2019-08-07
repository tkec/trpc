package com.github.trpc.common.exception;

public class RpcException extends RuntimeException {
    private Integer code;

    public static final int UNKNOWN_EXCEPTION = 0;
    public static final int NETWORK_EXCEPTION = 1;
    public static final int TIMEOUT_EXCEPTION = 2;
    public static final int SERVICE_EXCEPTION = 3;
    public static final int FORBIDDEN_EXCEPTION = 4;
    public static final int SERIALIZATION_EXCEPTION = 5;
    public static final int INTERCEPT_EXCEPTION = 6;

    public RpcException() {
        super();
    }

    public RpcException(String msg) {
        super(msg);
    }

    public RpcException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }

    public RpcException(Integer code) {
        super();
        this.code = code;
    }

    public RpcException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public RpcException(Integer code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }

    public RpcException(Integer code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }


    public boolean isServiceException() {
        return code == SERVICE_EXCEPTION;
    }

    public boolean isForbidded() {
        return code == FORBIDDEN_EXCEPTION;
    }

    public boolean isTimeout() {
        return code == TIMEOUT_EXCEPTION;
    }

    public boolean isNetwork() {
        return code == NETWORK_EXCEPTION;
    }

    public boolean isSerialization() {
        return code == SERIALIZATION_EXCEPTION;
    }
}
