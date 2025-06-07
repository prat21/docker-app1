package com.demo.docker.controller;

import com.demo.docker.service.TestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @GetMapping("/bucket/files")
    public Set<String> listFilesOfCloudStorageBucket(@RequestParam String volume) {
        File directory = new File(volume);
        var files = directory.listFiles();
        return Stream.of(files)
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }
}
