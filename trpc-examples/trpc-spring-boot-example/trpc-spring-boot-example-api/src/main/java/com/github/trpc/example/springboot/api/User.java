package com.github.trpc.example.springboot.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class User implements Serializable {
    private String userName;
    private Integer age;
}
