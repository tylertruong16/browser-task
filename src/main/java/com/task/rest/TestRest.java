package com.task.rest;

import com.task.model.BrowserTask;
import com.task.service.ChromeService;
import com.task.service.FirebaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestRest {

    final FirebaseService firebaseService;

    final ChromeService chromeService;

    public TestRest(ChromeService chromeService, FirebaseService firebaseService) {
        this.chromeService = chromeService;
        this.firebaseService = firebaseService;
    }


    @PostMapping("/save")
    public Object saveData(@RequestBody BrowserTask data) {
        firebaseService.saveBrowserTask(data);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }


}
