package com.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrowserTask {
    private String taskId;
    private long updateAt;
    private String status = TaskStatus.NEW.name();
    private String processStep = ActionStep.APP_STARTED.name();
    private String serverIp = "";

    public boolean newTask() {
        return StringUtils.equals(status, TaskStatus.NEW.name());
    }

    public boolean canNotStartBrowser() {
        return !StringUtils.equalsIgnoreCase(ActionStep.CONNECT_GOOGLE.name(), processStep);
    }
}
