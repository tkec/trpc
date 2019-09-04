package com.github.trpc.springboot.annotation;

import com.github.trpc.core.client.Endpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RpcAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
        implements MergedBeanDefinitionPostProcessor, BeanFactoryAware,
        PriorityOrdered, DisposableBean, ApplicationListener<ApplicationEvent> {

    private DefaultListableBeanFactory beanFactory;

    private List<RpcClientFactoryBean> rpcClientFactoryBeanList = new ArrayList<>();

    private Map<Integer, RpcServiceExporter> rpcServiceExporterMap = new HashMap<>();

    private AtomicBoolean started = new AtomicBoolean(false);

    private final Map<Class<?>, InjectionMetadata> injectionMetadataCache =
            new ConcurrentHashMap<Class<?>, InjectionMetadata>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();
        Annotation a = clazz.getAnnotation(TRpcService.class);
        if (a != null && a instanceof TRpcService) {
            log.debug("begin to process TRpcService");
            TRpcService rpcServiceAnnotation = (TRpcService) a;
            processRpcServiceAnnotation(rpcServiceAnnotation, beanFactory.getBean(beanName));
        }
        return bean;
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition rootBeanDefinition, Class<?> beanType, String beanName) {
        List<Class<? extends Annotation>> annotations = new ArrayList<>();
        annotations.add(TRpcClient.class);
        if (beanType != null && annotations != null) {
            InjectionMetadata metadata = findAnnotationMetadata(beanType, annotations);
            metadata.checkConfigMembers(rootBeanDefinition);
        }
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        List<Class<? extends Annotation>> annotations = new ArrayList<>();
        annotations.add(TRpcClient.class);

        InjectionMetadata metadata = findAnnotationMetadata(bean.getClass(), annotations);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Autowiring of methods failed", ex);
        }

        return pvs;
    }

    private InjectionMetadata findAnnotationMetadata(final Class clazz,
                                                     final List<Class<? extends Annotation>> annotation) {
        // Quick check on the concurrent map first, with minimal locking.
        InjectionMetadata metadata = this.injectionMetadataCache.get(clazz);
        if (metadata == null) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(clazz);
                if (metadata == null) {
                    LinkedList<InjectionMetadata.InjectedElement> elements;
                    elements = new LinkedList<InjectionMetadata.InjectedElement>();
                    parseFields(clazz, annotation, elements);
                    parseMethods(clazz, annotation, elements);

                    metadata = new InjectionMetadata(clazz, elements);
                    this.injectionMetadataCache.put(clazz, metadata);
                }
            }
        }
        return metadata;
    }

    protected void parseMethods(final Class<?> clazz, final List<Class<? extends Annotation>> annotions,
                                final LinkedList<InjectionMetadata.InjectedElement> elements) {
        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) {
                for (Class<? extends Annotation> anno : annotions) {
                    Annotation annotation = method.getAnnotation(anno);
                    if (annotation != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            throw new IllegalStateException("Autowired annotation is not supported on static methods");
                        }
                        if (method.getParameterTypes().length == 0) {
                            throw new IllegalStateException(
                                    "Autowired annotation requires at least one argument: " + method);
                        }
                        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
                        elements.add(new AutowiredMethodElement(method, annotation, pd));
                    }
                }
            }
        });
    }

    protected void parseFields(final Class<?> clazz, final List<Class<? extends Annotation>> annotations,
                               final LinkedList<InjectionMetadata.InjectedElement> elements) {
        ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) {
                for (Class<? extends Annotation> anno : annotations) {
                    Annotation annotation = field.getAnnotation(anno);
                    if (annotation != null) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            throw new IllegalStateException("Autowired annotation is not supported on static fields");
                        }
                        elements.add(new AutowiredFieldElement(field, annotation));
                    }
                }
            }
        });
    }

    private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

        /** target annotation type. */
        private final Annotation annotation;

        /**
         * Constructor with field and annotation type.
         *
         * @param field field instance
         * @param annotation annotation type
         */
        public AutowiredFieldElement(Field field, Annotation annotation) {
            super(field, null);
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            Field field = (Field) this.member;
            try {
                ReflectionUtils.makeAccessible(field);
                Object value = field.get(bean);
                Class serviceInterface = field.getType();
                value = processRpcClientAnnotation(annotation, serviceInterface, value);
                if (value != null) {
                    ReflectionUtils.makeAccessible(field);
                    field.set(bean, value);
                }
            } catch (Throwable ex) {
                throw new BeanCreationException("Could not autowire field: " + field, ex);
            }
        }
    }

    private class AutowiredMethodElement extends InjectionMetadata.InjectedElement {

        /** target annotation type. */
        private final Annotation annotation;

        /**
         * Constructor with method and annotation type.
         *
         * @param method method instance
         * @param annotation annotation type
         * @param pd {@link PropertyDescriptor} instance.
         */
        public AutowiredMethodElement(Method method, Annotation annotation, PropertyDescriptor pd) {
            super(method, pd);
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            if (this.skip == null && this.pd != null && pvs != null && pvs.contains(this.pd.getName())) {
                // Explicit value provided as part of the bean definition.
                this.skip = Boolean.TRUE;
            }
            if (this.skip != null && this.skip.booleanValue()) {
                return;
            }
            Method method = (Method) this.member;
            try {
                Class[] paramTypes = method.getParameterTypes();
                Object[] arguments = new Object[paramTypes.length];
                Class serviceInterface = method.getParameterTypes()[0];

                for (int i = 0; i < arguments.length; i++) {
                    MethodParameter methodParam = new MethodParameter(method, i);
                    GenericTypeResolver.resolveParameterType(methodParam, bean.getClass());
                    arguments[i] = processRpcClientAnnotation(annotation, serviceInterface, null);

                    if (arguments[i] == null) {
                        arguments = null;
                        break;
                    }
                }

                if (this.skip == null) {
                    if (this.pd != null && pvs instanceof MutablePropertyValues) {
                        ((MutablePropertyValues) pvs).registerProcessedProperty(this.pd.getName());
                    }
                    this.skip = Boolean.FALSE;
                }
                if (arguments != null) {
                    ReflectionUtils.makeAccessible(method);
                    method.invoke(bean, arguments);
                }
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            } catch (Throwable ex) {
                throw new BeanCreationException("Could not autowire method: " + method, ex);
            }
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof DefaultListableBeanFactory)) {
            throw new IllegalArgumentException("requires a DefaultListableBeanFactory");
        }
        this.beanFactory = (DefaultListableBeanFactory)beanFactory;
    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextStartedEvent || applicationEvent instanceof ContextRefreshedEvent) {
            // only execute this method once. bug fix for ContextRefreshedEvent will invoke twice on spring MVC servlet
            if (started.compareAndSet(false, true)) {
                try {
                    for (RpcServiceExporter exporter : rpcServiceExporterMap.values()) {
                        exporter.start();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                log.warn("onApplicationEvent of application event [" + applicationEvent
                        + "] ignored due to processor already started.");
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 3;
    }


    private void processRpcServiceAnnotation(TRpcService rpcService, Object bean) {
        TrpcProperties properties = beanFactory.getBean(TrpcProperties.class);
        if (properties == null) {
            throw new RuntimeException("trpc properties is null");
        }
        Integer port = properties.getServer().getPort();
        RpcServiceExporter rpcServiceExporter = rpcServiceExporterMap.get(port);
        if (rpcServiceExporter == null) {
            rpcServiceExporter = new RpcServiceExporter();
            rpcServiceExporterMap.put(port, rpcServiceExporter);
            rpcServiceExporter.setPort(port);
        }
        rpcServiceExporter.getRegisterService().add(bean);
    }

    private Object processRpcClientAnnotation(Annotation annotation, Class serviceInterface, Object value) throws Exception {
        if (!(annotation instanceof TRpcClient)) {
            return value;
        }
        TrpcProperties properties = beanFactory.getBean(TrpcProperties.class);
        if (properties == null) {
            throw new RuntimeException("trpc properties is null");
        }
        RpcClientFactoryBean rpcProxyFactoryBean;
        String factoryBeanName = "&" + serviceInterface.getSimpleName();
        try {
            rpcProxyFactoryBean = beanFactory.getBean(factoryBeanName, RpcClientFactoryBean.class);
            if (rpcProxyFactoryBean != null) {
                return rpcProxyFactoryBean.getObject();
            }
        } catch (NoSuchBeanDefinitionException ex) {
            // continue the following logic to create new factory bean
        }

        rpcProxyFactoryBean = createRpcClientFactoryBean(properties.getClient().getServiceUrl(), serviceInterface);
        rpcClientFactoryBeanList.add(rpcProxyFactoryBean);
        Object object = rpcProxyFactoryBean.getObject();

        return object;
    }

    private RpcClientFactoryBean createRpcClientFactoryBean(String serviceUrl, Class serviceInterface) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(RpcClientFactoryBean.class);
        MutablePropertyValues values = new MutablePropertyValues();
        values.addPropertyValue("serviceInterface", serviceInterface);
        String[] hostPort = serviceUrl.split(":");
        if (hostPort == null || hostPort.length != 2) {
            throw new IllegalArgumentException("service url is illegal. serviceUrl=" + serviceUrl);
        }
        Integer port = Integer.valueOf(hostPort[1]);
        values.addPropertyValue("endpoint", new Endpoint(hostPort[0], port));
        beanDefinition.setPropertyValues(values);
        String serviceInterfaceBeanName = serviceInterface.getSimpleName();
        beanFactory.registerBeanDefinition(serviceInterfaceBeanName, beanDefinition);
        return beanFactory.getBean("&" + serviceInterfaceBeanName, RpcClientFactoryBean.class);
    }
}
