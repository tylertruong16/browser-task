package com.task.repo;

import com.task.common.HttpUtil;
import com.task.common.JsonConverter;
import com.task.model.BrowserTask;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

@Repository
@Log
public class BrowserTaskRepo {

    private static final String HEADER_KEY_NAME = "realm";

    @Value("${system.id}")
    private String headerKey;
    @Value("${system.database-json-url}")
    private String databaseJsonUrl;

    public boolean saveBrowserTask(BrowserTask item) {
        var logId = UUID.randomUUID().toString();
        var json = JsonConverter.convertObjectToJson(item);
        var tableUrl = MessageFormat.format("{0}/browser-task", databaseJsonUrl);
        try {
            var url = MessageFormat.format("{0}/{1}", tableUrl, "insert");
            var header = HttpUtil.getHeaderPostRequest();
            header.add(HEADER_KEY_NAME, headerKey);
            log.log(Level.INFO, "browser-task >> saveBrowserTask >> json: {0} >> logId: {1}", new Object[]{JsonConverter.convertObjectToJson(item), logId});
            var response = HttpUtil.sendPostRequest(url, json, header).getBody();
            log.log(Level.INFO, "browser-task >> saveBrowserTask >> json: {0} >> logId: {1} >> response: {2}", new Object[]{JsonConverter.convertObjectToJson(item), logId, response});
            var jsonObject = new JSONObject(response);
            return jsonObject.has("updated");
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("browser-task >> saveBrowserTask >> logId: {0} >> json: {1} >> Exception:", logId, json), e);
            return false;
        }
    }

    public List<BrowserTask> getBrowserTaskById(String id) {
        var logId = UUID.randomUUID().toString();
        var tableUrl = MessageFormat.format("{0}/browser-task", databaseJsonUrl);
        var param = MessageFormat.format("search?filters=id::::{0}", id);
        var url = MessageFormat.format("{0}/{1}", tableUrl, param);
        return getBrowserTasks(url, logId);
    }

    private List<BrowserTask> getBrowserTasks(String url, String logId) {
        try {
            var header = HttpUtil.getHeaderPostRequest();
            header.add(HEADER_KEY_NAME, headerKey);
            log.log(Level.INFO, "browser-task >> saveBrowserTask >> url: {0} >> logId: {1}", new Object[]{url, logId});
            var response = HttpUtil.sendRequest(url, header).getBody();
            log.log(Level.INFO, "browser-task >> saveBrowserTask >> url: {0} >> logId: {1} >> response: {2}", new Object[]{url, logId, response});
            var jsonObject = new JSONObject(response);
            var data = jsonObject.getJSONArray("data").toString();
            return Arrays.stream(JsonConverter.convertToObject(data, BrowserTask[].class)
                    .orElse(new BrowserTask[]{})).toList();
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("browser-task >> getBrowserTaskById >> logId: {0} >> url: {1} >> Exception:", logId, url), e);
            return new ArrayList<>();
        }
    }

    public void updateTaskStatus(String taskId, String status) {
        var logId = UUID.randomUUID().toString();
        var tasks = getBrowserTaskById(taskId);
        if (CollectionUtils.isNotEmpty(tasks)) {
            tasks.forEach(it -> {
                var clone = SerializationUtils.clone(it);
                clone.setProcessStep(status);
                saveBrowserTask(clone);
            });

        } else {
            log.log(Level.WARNING, MessageFormat.format("browser-task >> updateTaskStatus >> logId: {0} >> taskId: {1} >> task id does not exist:", logId, taskId));
        }
    }

    public void removeRecordByServerIp(String serverIp) {
        var logId = UUID.randomUUID().toString();
        var tableUrl = MessageFormat.format("{0}/browser-task", databaseJsonUrl);
        var param = MessageFormat.format("search?filters=serverIp::::{0}", serverIp);
        var url = MessageFormat.format("{0}/{1}", tableUrl, param);
        var tasks = getBrowserTasks(url, logId);
        if (CollectionUtils.isNotEmpty(tasks)) {
            tasks.forEach(it -> {
                var clone = SerializationUtils.clone(it);
                clone.setDeleted(true);
                saveBrowserTask(clone);
            });

        } else {
            log.log(Level.WARNING, MessageFormat.format("browser-task >> removeRecordByServerIp >> logId: {0} >> serverIp: {1} >> server ip does not exist:", logId, serverIp));
        }
    }

}
