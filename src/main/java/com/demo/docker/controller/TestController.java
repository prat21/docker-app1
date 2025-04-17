package com.demo.docker.controller;

import com.demo.docker.service.TestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/docker/app1")
public class TestController {
    TestService testService;

    TestController(TestService testService){
        this.testService = testService;
    }

    @GetMapping("/test")
    public String test(){
        return "testing docker app 1";
    }

    @GetMapping("/connect/app2")
    public String connectApp2(){
        return testService.testConnect();
    }
}
