package com.github.trpc.example.springboot.client;

import com.github.trpc.example.springboot.api.EchoService;
import com.github.trpc.example.springboot.api.User;
import com.github.trpc.springboot.annotation.TrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class EchoController {

    @TrpcClient("spring-boot-trpc-server")
    private EchoService echoService;

    @GetMapping("/echo")
    public String ehco(String msg) {
        return echoService.echo(msg);
    }

    @GetMapping("/user")
    public User user(String userName, Integer age) {
        return echoService.getUser(userName, age);
    }
}
