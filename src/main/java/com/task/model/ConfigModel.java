package com.task.model;

import lombok.Data;

@Data
public class ConfigModel {
    private String id;
    private Object firebaseConfig;
    private String firebaseUrl;

}
