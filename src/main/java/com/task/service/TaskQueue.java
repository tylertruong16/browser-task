package com.task.service;

import com.task.common.CommonUtil;
import com.task.common.JsonConverter;
import com.task.model.BrowserTask;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

@Service
@Log
public class TaskQueue {


    private volatile boolean alive = true;
    final ChromeService chromeService;
    final AppStoreService appStoreService;

    @Getter
    final BlockingQueue<BrowserTask> queue = new LinkedBlockingDeque<>();

    public TaskQueue(ChromeService chromeService, AppStoreService appStoreService) {
        this.chromeService = chromeService;
        this.appStoreService = appStoreService;
    }

    @PostConstruct
    void init() {
        CompletableFuture.runAsync(() -> {
            try {
                while (alive) {
                    var data = queue.take();
                    this.handleBrowserTask(data);
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void pushTask(BrowserTask task) {
        if (task.newTask() && StringUtils.equalsIgnoreCase(task.getServerIp(), CommonUtil.getServerIP())) {
            // push to handle task
            log.log(Level.INFO, "browser-task >> FirebaseService >> task: {0}", JsonConverter.convertObjectToJson(task));
            queue.add(task);
        }
    }

    public void handleBrowserTask(BrowserTask browserTask) {
        var result = new ArrayList<BrowserTask>();
        try {
            var response = chromeService.handleBrowserTask(browserTask);
            Optional.ofNullable(response.getResponse()).ifPresent(result::add);
        } catch (Exception e) {
            log.log(Level.WARNING, "browser-task >> handleBrowserTask >> Exception:", e);
        } finally {
            if (CollectionUtils.isNotEmpty(result)) {
                var first = result.getFirst();
                appStoreService.saveBrowserTask(first);
            }

        }
    }

    @PreDestroy
    void destroy() {
        this.alive = false;
    }
}
