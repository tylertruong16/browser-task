package com.task.service;

import com.fasterxml.uuid.Generators;
import com.task.common.CommonUtil;
import com.task.model.ActionStep;
import com.task.model.AppId;
import com.task.model.BrowserTask;
import com.task.repo.BrowserTaskRepo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
@Log
public class AppStoreService {

    @Autowired
    AppId appId;


    @Value("${system.virtual-url}")
    private String virtualUrl;

    final BrowserTaskRepo browserTaskRepo;

    public AppStoreService(BrowserTaskRepo browserTaskRepo) {
        this.browserTaskRepo = browserTaskRepo;
    }


    public void updateTaskStatus(String taskId, String status) {
        browserTaskRepo.updateTaskStatus(taskId, status);
    }


    public void saveBrowserTask(BrowserTask browserTask) {
        browserTaskRepo.saveBrowserTask(browserTask);
    }


    public void removeRecordByServerIp(String serverIp) {
        browserTaskRepo.removeRecordByServerIp(serverIp);
    }


    @PostConstruct
    public void init() {
        var taskId = appId.getId();
        var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        var initTask = new BrowserTask(taskId,
                taskId,
                ActionStep.APP_STARTED.name(),
                CommonUtil.getServerIP(),
                virtualUrl,
                "",
                false, updateAt, updateAt);
        saveBrowserTask(initTask);
    }


    @PreDestroy
    void destroy() {
        removeRecordByServerIp(CommonUtil.getServerIP());
    }


}
