package com.demo.docker.controller;

import com.demo.docker.service.TestService;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/docker/app1")
public class TestController {
    TestService testService;
    Environment env;

    TestController(TestService testService, Environment env){
        this.testService = testService;
        this.env = env;
    }

    @GetMapping("/test")
    public String test(){
        System.out.println(env.getProperty("app2.host"));
        return "testing docker app 1";
    }

    @GetMapping("/connect/app2")
    public String connectApp2(){
        return testService.testConnect();
    }
}
