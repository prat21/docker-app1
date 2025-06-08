package com.demo.docker.controller;

import com.demo.docker.service.TestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    @Value("${file.upload.path}")
    private String uploadPath;

    TestController(TestService testService){
        this.testService = testService;
    }

    @GetMapping("/test")
    public String test(){
        System.out.println("Inside test");
        return reloadVal;
    }

    @GetMapping("/connect/app2")
    public String connectApp2(){
        return testService.testConnect();
    }

    @GetMapping("/bucket/files")
    public Set<String> listFilesOfCloudStorageBucket(@RequestParam String volume) {
        System.out.println("Inside bucket files");
        File directory = new File(volume);
        var files = directory.listFiles();
        return Stream.of(files)
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    @PostMapping("/upload")
    public void uploadFile(@RequestParam MultipartFile file){
        System.out.println("Inside upload");
        System.out.println(uploadPath +"/"+ file.getOriginalFilename());
        File myFile = new File(uploadPath +"/"+ file.getOriginalFilename());
        try(OutputStream outputStream = new FileOutputStream(myFile)){
            outputStream.write(file.getBytes());
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
