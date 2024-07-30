package com.task.rest;

import com.task.model.BrowserTask;
import com.task.service.ChromeService;
import com.task.service.FirebaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TestRest {

    final FirebaseService firebaseService;

    final ChromeService chromeService;

    public TestRest(ChromeService chromeService, FirebaseService firebaseService) {
        this.chromeService = chromeService;
        this.firebaseService = firebaseService;
    }

    @GetMapping(value = "/test-chrome")
    public Object testAccessChrome(@RequestParam String email) {
        chromeService.accessGoogle(email);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    @PostMapping("/save")
    public Object saveData(@RequestBody BrowserTask.Request data) {
        var browserTask = data.convertToBrowserTask();
        firebaseService.saveBrowserTask(browserTask);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }


}
