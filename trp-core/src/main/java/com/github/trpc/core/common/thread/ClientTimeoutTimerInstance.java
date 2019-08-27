package com.github.trpc.core.common.thread;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

public class ClientTimeoutTimerInstance {

    private static volatile Timer timeoutTimer;

    private ClientTimeoutTimerInstance() {

    }

    public static Timer getInstance() {
        if (timeoutTimer == null) {
            synchronized (ClientTimeoutTimerInstance.class) {
                if (timeoutTimer == null) {
                    timeoutTimer = new HashedWheelTimer(new CustomThreadFactory("timeout-timer-thread"));
                }
            }
        }
        return timeoutTimer;
    }
}
