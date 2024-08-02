package com.task.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.task.common.CommonUtil;
import com.task.common.JsonConverter;
import com.task.model.ActionStep;
import com.task.model.BrowserTask;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

@Service
@Log
public class TaskQueue {


    final DatabaseReference databaseReference;
    private volatile boolean alive = true;
    final ChromeService chromeService;
    final FirebaseService firebaseService;

    @Getter
    final BlockingQueue<BrowserTask> queue = new LinkedBlockingDeque<>();

    public TaskQueue(DatabaseReference databaseReference, ChromeService chromeService, FirebaseService firebaseService) {
        this.databaseReference = databaseReference;
        this.chromeService = chromeService;
        this.firebaseService = firebaseService;
    }

    @PostConstruct
    void init(){
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
        subscribeToChanges();
    }

    private void subscribeToChanges() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    var task = Optional.ofNullable(ds.getValue(BrowserTask.class)).orElse(new BrowserTask());
                    if (task.newTask() && StringUtils.equalsIgnoreCase(task.getServerIp(), CommonUtil.getServerIP())) {
                        // push to handle task
                        log.log(Level.INFO, "browser-task >> FirebaseService >> task: {0}", JsonConverter.convertObjectToJson(task));
                        queue.add(task);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.log(Level.WARNING, "browser-task >> FirebaseService >> subscribeToChanges >> onCancelled >> Exception:", databaseError.toException());
            }
        });
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
                first.setProcessStep(ActionStep.APP_STARTED.name());
                first.setCurrentProfiles(CommonUtil.getAllFolderNames());
                firebaseService.saveBrowserTask(first);
            }

        }
    }

    @PreDestroy
    void destroy() {
        this.alive = false;
    }
}
