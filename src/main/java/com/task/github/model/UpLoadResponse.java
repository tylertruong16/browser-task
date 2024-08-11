package com.task.github.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpLoadResponse {
    @Builder.Default
    private String fileName = "";
    @Builder.Default
    private boolean status = false;
}
