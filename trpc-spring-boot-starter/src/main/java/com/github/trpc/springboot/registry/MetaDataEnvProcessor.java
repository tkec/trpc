package com.github.trpc.springboot.registry;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class MetaDataEnvProcessor implements EnvironmentPostProcessor {

    private static final String PORT_KEY = "trpc.server.port";
    public static final String META_PORT_KEY = "trpcPort";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {

        String port = environment.getProperty(PORT_KEY);

        if (StringUtils.isNotEmpty(port)) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            Map<String, String> metaMap = new HashMap<>();
            metaMap.put(META_PORT_KEY, port);
            map.put("eureka.instance.metadata-map", metaMap);
            MapPropertySource propertySource = new MapPropertySource("eurekaMetadataTrpcPort", map);
            environment.getPropertySources().addLast(propertySource);
        }
    }
}