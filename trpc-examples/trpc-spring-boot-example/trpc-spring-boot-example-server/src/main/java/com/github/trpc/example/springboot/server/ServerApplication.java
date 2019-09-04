package com.github.trpc.example.springboot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ServerApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);

        synchronized (ServerApplication.class) {
            try {
                ServerApplication.class.wait();
            } catch (Exception e) {

            }
        }
    }
}
