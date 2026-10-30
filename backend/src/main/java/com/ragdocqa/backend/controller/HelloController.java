package com.ragdocqa.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    //@RestController means "this class handles HTTP requests, 
    // and each method's return value is the response body.
    // " Without it, Spring would try to find an HTML page instead.

    //@GetMapping("/hello") makes a GET /hello request call this method.
    @GetMapping("/hello")
    public String hello() { 
        return "Hello from rag-doc-qa";
    }
    
}
