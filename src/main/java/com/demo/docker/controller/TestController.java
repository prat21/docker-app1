package com.demo.docker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/docker/app1")
public class TestController {

    @GetMapping("/test")
    public String test(){
        return "testing docker app 1";
    }
}
