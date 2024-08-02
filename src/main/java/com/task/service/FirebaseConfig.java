package com.task.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.database-url}")
    private String firebaseDatabaseUrl;

    @Value("${firebase.collection-name}")
    private String firebaseCollectionName;

    @Bean
    public DatabaseReference databaseReference() throws IOException {
        initializeFirebase();
        var firebaseDatabase = FirebaseDatabase.getInstance();
        return firebaseDatabase.getReference(firebaseCollectionName);
    }

    private void initializeFirebase() throws IOException {
        var serviceAccount = new FileInputStream("src/main/resources/google-services.json");

        var options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl(firebaseDatabaseUrl)
                .build();
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
