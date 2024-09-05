package com.task.rest;

import com.task.model.BrowserTask;
import com.task.service.ChromeService;
import com.task.service.AppStoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestRest {

    final AppStoreService appStoreService;

    final ChromeService chromeService;

    public TestRest(ChromeService chromeService, AppStoreService appStoreService) {
        this.chromeService = chromeService;
        this.appStoreService = appStoreService;
    }


    @PostMapping("/save")
    public Object saveData(@RequestBody BrowserTask data) {
        appStoreService.saveBrowserTask(data);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }


}
