package com.github.trpc.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Endpoint {
    private String host;
    private Integer port;
}
