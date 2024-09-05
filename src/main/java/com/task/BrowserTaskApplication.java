package com.task;

import com.fasterxml.uuid.Generators;
import com.task.model.AppId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrowserTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrowserTaskApplication.class, args);
    }

    @Bean
    public AppId myBean() {
        var taskId = Generators.timeBasedEpochGenerator().generate().toString();
        return new AppId(taskId);
    }

}
