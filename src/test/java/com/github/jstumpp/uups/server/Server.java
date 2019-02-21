package com.github.jstumpp.uups.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.github.jstumpp.uups"})
public class Server {
    
    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }
}
