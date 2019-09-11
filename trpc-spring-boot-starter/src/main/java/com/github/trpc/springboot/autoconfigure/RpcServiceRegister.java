package com.github.trpc.springboot.autoconfigure;

import com.github.trpc.springboot.annotation.TrpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RpcServiceRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware,
        EnvironmentAware, BeanFactoryAware {

    private ResourceLoader resourceLoader;

    private Environment environment;

    private BeanFactory beanFactory;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        Map<Class, String> serviceExporterMap = new HashMap<>();
        AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
        Collection<BeanDefinition> candidates = getCandidates(resourceLoader);
        for (BeanDefinition candidate : candidates) {
            Class<?> clazz = getClass(candidate.getBeanClassName());
            Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(clazz);
            if (interfaces.length != 1) {
                throw new BeanInitializationException("bean interface num must equal 1, " + clazz.getName());
            }
            String serviceBeanName = beanNameGenerator.generateBeanName(candidate, beanDefinitionRegistry);
            String old = serviceExporterMap.putIfAbsent(interfaces[0], serviceBeanName);
            if (old != null) {
                throw new RuntimeException("interface already be exported by bean name:" + old);
            }
            log.debug("register TrpcService bean, serviceBeanName:" + serviceBeanName + ", candidate:" + candidate);
            beanDefinitionRegistry.registerBeanDefinition(serviceBeanName, candidate);
        }
    }

    private Collection<BeanDefinition> getCandidates(ResourceLoader resourceLoader) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);

        scanner.addIncludeFilter(new AnnotationTypeFilter(TrpcService.class));
        scanner.setResourceLoader(resourceLoader);
        return AutoConfigurationPackages.get(beanFactory).stream()
                .flatMap(basePackage -> scanner.findCandidateComponents(basePackage).stream())
                .collect(Collectors.toSet());
    }

    private Class<?> getClass(String beanClassName) {
        try {
            return Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            throw new BeanInitializationException("error create bean with class: " + beanClassName, e);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
