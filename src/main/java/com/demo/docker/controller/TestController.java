package com.demo.docker.controller;

import com.demo.docker.service.TestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/docker/app1")
@RefreshScope
public class TestController {
    TestService testService;
    @Value("${reload.test}")
    private String reloadVal;

    TestController(TestService testService){
        this.testService = testService;
    }

    @GetMapping("/test")
    public String test(){
        return reloadVal;
    }

    @GetMapping("/connect/app2")
    public String connectApp2(){
        return testService.testConnect();
    }
}
