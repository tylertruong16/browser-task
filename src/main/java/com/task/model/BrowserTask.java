package com.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrowserTask implements Serializable {

    private String taskId;
    private String processStep = ActionStep.APP_STARTED.name();
    private String serverIp = "";
    private String virtualUrl = "";

    public boolean newTask() {
        return Stream.of(ActionStep.APP_STARTED, ActionStep.CONNECT_GOOGLE)
                .anyMatch(it -> StringUtils.equals(processStep, it.name()));

    }

    public boolean canNotStartBrowser() {
        return !StringUtils.equalsIgnoreCase(ActionStep.CONNECT_GOOGLE.name(), processStep);
    }
}
