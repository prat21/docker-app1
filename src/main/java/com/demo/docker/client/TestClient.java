package com.demo.docker.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "testClient", url = "http://${app2.host}:8082/docker/app2")
public interface TestClient {
    @GetMapping("/test")
    String connectApp2();
}
