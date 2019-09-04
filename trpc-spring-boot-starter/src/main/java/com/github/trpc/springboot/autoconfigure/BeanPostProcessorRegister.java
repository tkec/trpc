package com.github.trpc.springboot.autoconfigure;

import com.github.trpc.springboot.annotation.RpcAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class BeanPostProcessorRegister implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(RpcAnnotationBeanPostProcessor.class);
        beanDefinitionRegistry.registerBeanDefinition("rpcAnnotationBeanPostProcessor", beanDefinition);
    }
}
