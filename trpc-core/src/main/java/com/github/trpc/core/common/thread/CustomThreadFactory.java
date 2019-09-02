package com.github.trpc.core.common.thread;

import io.netty.util.concurrent.FastThreadLocalThread;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CustomThreadFactory implements ThreadFactory {

    private AtomicInteger threadNumber = new AtomicInteger(1);
    private String namePrefix;
    private ThreadGroup group;

    public CustomThreadFactory(String namePrefix) {
        SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix + "-";
    }

    public Thread newThread(Runnable r) {
        String name = namePrefix + threadNumber.getAndIncrement();
        Thread t = new FastThreadLocalThread(group, r, name, 0);
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

}