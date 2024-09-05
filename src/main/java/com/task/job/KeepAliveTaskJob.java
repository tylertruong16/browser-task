package com.task.job;

import com.task.common.CommonUtil;
import com.task.model.AppId;
import com.task.model.BrowserTask;
import com.task.repo.BrowserTaskRepo;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class KeepAliveTaskJob {

    final AppId appId;
    final BrowserTaskRepo browserTaskRepo;

    public KeepAliveTaskJob(AppId appId, BrowserTaskRepo browserTaskRepo) {
        this.appId = appId;
        this.browserTaskRepo = browserTaskRepo;
    }

    @Scheduled(initialDelay = 1, fixedDelay = 3, timeUnit = TimeUnit.MINUTES)
    void keepAliveProfile() {
        var taskId = appId.getId();
        try {
            var tasks = browserTaskRepo.getBrowserTaskById(taskId)
                    .stream().filter(BrowserTask::taskCanUpdateKeepAlive);
            tasks.forEach(task -> {
                var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                var clone = SerializationUtils.clone(task);
                clone.setUpdatedAt(updateAt);
                log.log(Level.INFO, "browser-task >> keepAliveProfile >> app : {0} >> body: {1}", new Object[]{task.getId(), clone});
                browserTaskRepo.saveBrowserTask(clone);
            });
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("browser-task >> keepAliveProfile >> taskId: {0} >> Exception:", taskId), e);
        }
    }

    @Scheduled(initialDelay = 1, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void cleanUpPendingApp() {
        try {
            var tasks = browserTaskRepo.getBrowserTaskNotDeleted()
                    .stream().filter(BrowserTask::taskCanUpdateKeepAlive)
                    .filter(it -> StringUtils.isBlank(it.getUpdatedAt()) || CommonUtil.isMoreThan30MinutesAhead(it.getUpdatedAt()))
                    .toList();
            tasks.forEach(task -> {
                var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                var clone = SerializationUtils.clone(task);
                clone.setUpdatedAt(updateAt);
                clone.setDeleted(true);
                log.log(Level.INFO, "browser-task >> cleanUpPendingApp >> remove app : {0} >> body: {1}", new Object[]{task.getId(), clone});
                browserTaskRepo.saveBrowserTask(clone);
            });
        } catch (Exception e) {
            log.log(Level.WARNING, "browser-task >> cleanUpPendingApp >> Exception:", e);
        }
    }
}
