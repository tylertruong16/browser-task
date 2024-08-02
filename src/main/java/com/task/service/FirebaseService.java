package com.task.service;

import com.fasterxml.uuid.Generators;
import com.google.firebase.database.*;
import com.task.common.CommonUtil;
import com.task.model.ActionStep;
import com.task.model.BrowserTask;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.logging.Level;

@Service
@Log
public class FirebaseService {

    final DatabaseReference databaseReference;


    public FirebaseService(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
    }

    public void updateTaskStatus(String taskId, String status) {
        var taskIdRef = databaseReference.child(taskId);
        taskIdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // taskId exists, update status
                    log.log(Level.INFO, "browser-task >> updateTaskStatus >> update status >> taskId: {0} >> newStatus >> {1}", new Object[]{taskId, status});
                    taskIdRef.child("processStep").setValueAsync(status);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                log.log(Level.WARNING, "browser-task >> FirebaseService >> updateTaskStatus >> onCancelled >> Exception:", databaseError.toException());
            }
        });
    }


    public void saveBrowserTask(BrowserTask browserTask) {
        var taskIdRef = databaseReference.child(browserTask.getTaskId());
        taskIdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // taskId exists, update status
                    log.log(Level.INFO, "browser-task >> taskId exists >> update status >> taskId: {0} >> status: {1}", new Object[]{browserTask.getTaskId(), browserTask.getProcessStep()});
                    taskIdRef.child("processStep").setValueAsync(browserTask.getProcessStep());
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
    public void init() {
        var taskId = Generators.timeBasedEpochGenerator().generate().toString();
        var initTask = new BrowserTask(taskId, ActionStep.APP_STARTED.name(), CommonUtil.getServerIP());
        saveBrowserTask(initTask);
    }


    @PreDestroy
    void destroy() {
        removeRecordByServerIp(CommonUtil.getServerIP());
    }


}
