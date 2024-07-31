package com.task.service;

import com.fasterxml.uuid.Generators;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import com.task.common.CommonUtil;
import com.task.common.JsonConverter;
import com.task.model.ActionStep;
import com.task.model.BrowserTask;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

@Service
@Log
public class FirebaseService {
    @Value("${firebase.database-url}")
    private String firebaseDatabaseUrl;

    @Value("${firebase.collection-name}")
    private String firebaseCollectionName;

    DatabaseReference databaseReference;
    final ChromeService chromeService;

    private volatile boolean alive = true;

    @Getter
    final BlockingQueue<BrowserTask> queue = new LinkedBlockingDeque<>();

    public FirebaseService(ChromeService chromeService) {
        this.chromeService = chromeService;
    }


    public void saveBrowserTask(BrowserTask browserTask) {
        var taskIdRef = databaseReference.child(browserTask.getTaskId());
        taskIdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // taskId exists, update status
                    log.log(Level.INFO, "browser-task >> taskId exists >> update status >> taskId: {0}", browserTask.getTaskId());
                    taskIdRef.child("updateAt").setValueAsync(browserTask.getUpdateAt());
                    taskIdRef.child("processStep").setValueAsync(browserTask.getProcessStep());
                    taskIdRef.child("stepHistory").setValueAsync(browserTask.getStepHistory());
                    taskIdRef.child("currentProfiles").setValueAsync(browserTask.getCurrentProfiles());
                } else {
                    // taskId does not exist, save new task
                    removeRecordByServerIp(browserTask.getServerIp());
                    log.log(Level.INFO, "browser-task >> save task >> taskId: {0}", browserTask.getTaskId());
                    databaseReference.child(browserTask.getTaskId()).setValueAsync(browserTask);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.log(Level.WARNING, "browser-task >> FirebaseService >> saveBrowserTask >> onCancelled >> Exception:", databaseError.toException());
            }
        });
    }

    public void removeRecordByServerIp(String serverIp) {
        Query query = databaseReference.orderByChild("serverIp").equalTo(serverIp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValueAsync();
                }
                log.log(Level.INFO, "browser-task >> FirebaseService >> removeRecordByServerIp >> Record(s) with serverIp: {0} >> removed successfully", serverIp);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.log(Level.WARNING, "browser-task >> FirebaseService >> removeRecordByServerIp >> onCancelled >> Exception:", databaseError.toException());
            }
        });
    }


    @PostConstruct
    public void init() throws IOException {
        var serviceAccount = new FileInputStream("src/main/resources/google-services.json");

        var options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl(firebaseDatabaseUrl)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
        var firebaseDatabase = FirebaseDatabase.getInstance();
        this.databaseReference = firebaseDatabase.getReference(firebaseCollectionName);
        var taskId = Generators.timeBasedEpochGenerator().generate().toString();
        var initTask = new BrowserTask(taskId, System.currentTimeMillis(), ActionStep.APP_STARTED.name(), CommonUtil.getServerIP(), CommonUtil.getAllFolderNames(), new ArrayList<>());
        saveBrowserTask(initTask);
        subscribeToChanges();
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
                saveBrowserTask(first);
            }

        }
    }

    @PreDestroy
    void destroy() {
        this.alive = false;
    }


}
