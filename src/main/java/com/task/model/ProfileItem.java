package com.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileItem implements Serializable {
    private String id = "";
    private String email = "";
    private String status = "";
    private String updateDate = "";
    private String profileFolderUrl = "";
    private String username = "";


    public static ProfileItem createOfflineProfile(String email, String username) {
        var updateAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        var id = new String(Base64.getEncoder().encode(email.getBytes(StandardCharsets.UTF_8)));
        return new ProfileItem(id, email, "OFFLINE", updateAt, "", username);
    }

    public boolean notUpdateProfileFolder() {
        return StringUtils.isBlank(this.getProfileFolderUrl()) && StringUtils.isNoneBlank(this.getUpdateDate());
    }

    public ZonedDateTime parseUpdateDate() {
        try {
            var formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
            return ZonedDateTime.parse(this.getUpdateDate(), formatter);
        } catch (DateTimeParseException e) {
            return ZonedDateTime.now();
        }
    }

}
