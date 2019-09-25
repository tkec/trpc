package com.github.trpc.springboot.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

@Slf4j
public class ApplicationContextHolder implements ApplicationContextAware, PriorityOrdered, InitializingBean {
    private static ApplicationContext context;

    public ApplicationContextHolder() {
        System.out.println("begin to init applicationContextHolder");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.context = applicationContext;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("context:" + context);
        log.info("context holder:" + context);
    }
}
