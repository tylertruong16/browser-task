package com.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrowserTask {
    private String email;
    private long updateAt;
    private String status = TaskStatus.NEW.name();

    public String convertToPrimaryKey() {
        return Base64.getEncoder().encodeToString(email.getBytes());
    }

    public boolean newTask() {
        return StringUtils.equalsIgnoreCase(status, TaskStatus.NEW.name());
    }

    @Data
    public static class Request {
        private String email;
        private String status = TaskStatus.NEW.name();

        public BrowserTask convertToBrowserTask() {
            return new BrowserTask(email, System.currentTimeMillis(), status);
        }
    }

}
