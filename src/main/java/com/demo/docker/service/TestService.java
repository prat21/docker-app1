package com.demo.docker.service;

import com.demo.docker.client.TestClient;
import com.demo.docker.entity.Employee;
import com.demo.docker.repository.EmployeeRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestService {
    TestClient client;
    EmployeeRepo employeeRepo;

    TestService(TestClient client, EmployeeRepo employeeRepo){
        this.client = client;
        this.employeeRepo = employeeRepo;
    }

    public String testConnect(){
        var response = client.connectApp2();
        return response;
    }

    public List<Employee> getEmployees(){
        System.out.println("Inside getemployees in service");
        var employees = employeeRepo.findAll();
        System.out.println("Employees: "+employees);
        return employees;
    }
}
