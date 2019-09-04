package com.github.trpc.springboot.autoconfigure;

import com.github.trpc.springboot.annotation.RpcClientConfig;
import com.github.trpc.springboot.annotation.TrpcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableConfigurationProperties(TrpcProperties.class)
@Configuration
@Import({BeanPostProcessorRegister.class, RpcServiceRegister.class})
public class TrpcAutoConfiguration {

//    @Bean
//    RpcClientConfig getConfig() {
//        System.out.println("new config");
//        return new RpcClientConfig();
//    }
}
