package com.demo.docker.service;

import com.demo.docker.client.TestClient;
import org.springframework.stereotype.Service;

@Service
public class TestService {
    TestClient client;

    TestService(TestClient client){
        this.client = client;
    }

    public String testConnect(){
        var response = client.connectApp2();
        return response;
    }
}
