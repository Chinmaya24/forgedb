package com.minidb.minidb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
public class MinidbApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinidbApplication.class, args);
    }

    @GetMapping("/hello")
    public String hello(){
        return "MiniDB running";
    }
}