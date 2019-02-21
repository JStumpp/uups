package com.github.jstumpp.uups.server;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
public class Controller {
    @RequestMapping("/")
    public String index() {
        return String.format("Hello, world!");
    }

    @RequestMapping("/parseexception")
    public String parseexception() throws ParseException {
        throw new ParseException("Parseexception", 1);
    }
}
