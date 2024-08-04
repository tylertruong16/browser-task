package com.task.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.task.common.HttpUtil;
import com.task.common.JsonConverter;
import com.task.model.ConfigModel;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Log
public class FirebaseConfig {


    @Value("${spring.application.name}")
    private String firebaseCollectionName;

    @Value("${system.id}")
    private String headerKey;

    @Value("${system.config-url}")
    private String configUrl;

    @Bean
    public DatabaseReference databaseReference() {
        initializeFirebase();
        var firebaseDatabase = FirebaseDatabase.getInstance();
        return firebaseDatabase.getReference(firebaseCollectionName);
    }

    public ConfigModel loadConfig() {
        var header = HttpUtil.getHeaderPostRequest();
        header.add("realm", headerKey);
        var body = HttpUtil.sendRequest(configUrl, header).getBody();
        return JsonConverter.convertToObject(body, ConfigModel.class).stream().findFirst().orElseThrow();
    }

    @SneakyThrows
    public static FileInputStream convertJsonStringToFileInputStream(String jsonString, String filePath) {
        var file = new File(filePath);
        var parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
        }
        return new FileInputStream(file);
    }

    @SneakyThrows
    private void initializeFirebase() {
        var configModel = loadConfig();
        var json = JsonConverter.convertObjectToJson(configModel.getFirebaseConfig());
        var baseDirectoryPath = System.getProperty("user.home") + File.separator + "app-config" + File.separator + "google-services.json";
        var serviceAccount = convertJsonStringToFileInputStream(json, baseDirectoryPath);

        var options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl(configModel.getFirebaseUrl())
                .build();
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
