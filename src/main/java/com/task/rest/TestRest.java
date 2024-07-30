package com.task.rest;

import com.task.service.ChromeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestRest {

    final ChromeService chromeService;

    public TestRest(ChromeService chromeService) {
        this.chromeService = chromeService;
    }

    @GetMapping(value = "/test-chrome")
    public Object testAccessChrome(@RequestParam String email) {
        chromeService.accessGoogle(email);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }


}
