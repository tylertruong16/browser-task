package com.task.job;

import com.task.common.CommonUtil;
import com.task.common.FileSplitter;
import com.task.common.FileUtil;
import com.task.common.JsonConverter;
import com.task.github.GitHubService;
import com.task.github.model.ContentResponse;
import com.task.github.model.GitHubConfig;
import com.task.model.ProfileItem;
import com.task.service.ProfileManagerRepo;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Service
@Log
public class CollectProfileJob {

    final ProfileManagerRepo profileManagerRepo;
    @Value("${github.api-url}")
    private String githubApiUrl;
    @Value("${github.token}")
    private String githubToken;

    public CollectProfileJob(ProfileManagerRepo profileManagerRepo) {
        this.profileManagerRepo = profileManagerRepo;

    }


    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    void collectProfile() {
        try {
            var accounts = profileManagerRepo.getAllProfile().stream().filter(ProfileItem::notUpdateProfileFolder)
                    .sorted(Comparator.comparing(ProfileItem::parseUpdateDate))
                    .toList();
            accounts.forEach(it -> {
                var logId = UUID.randomUUID().toString();
                log.log(Level.INFO, "browser-task >> CollectProfileJob >> collectProfile >> logId: {0} >> email: {1}", new Object[]{logId, it.getEmail()});
                try {
                    zipProfiles(it, logId);
                } catch (Exception e) {
                    log.log(Level.WARNING, MessageFormat.format("browser-task >> CollectProfileJob >> collectProfile >> logId: {0} >> Exception:", logId), e);
                }

            });

        } catch (Exception e) {
            log.log(Level.WARNING, "browser-task >> CollectProfileJob >> collectProfile >> Exception:", e);
        }
    }

    @SneakyThrows
    public void zipProfiles(ProfileItem item, String logId) {
        var email = item.getEmail();
        // s1: zip file to folder user.home\zip-chrome-profiles\xxx.zip
        var baseDirectoryPath = System.getProperty("user.home") + File.separator + "chrome-profiles";
        var sourceFolder = MessageFormat.format("{0}{1}{2}", baseDirectoryPath, File.separator, email);
        var baseDirectoryZipPath = System.getProperty("user.home") + File.separator + "zip-chrome-profiles" + File.separator + email;
        if (!new File(sourceFolder).exists()) {
            log.log(Level.INFO, "browser-task >> CollectProfileJob >> zipProfiles >> folder profile not exist: {0}", sourceFolder);
            return;
        }
        CommonUtil.deleteFolder(new File(baseDirectoryZipPath));

        var zipFileUrl = MessageFormat.format("{0}{1}{2}.zip", baseDirectoryZipPath, File.separator, email);
        FileUtil.zipFolder(sourceFolder, zipFileUrl);
        var response = FileSplitter.splitFile(zipFileUrl, 10);
        log.log(Level.INFO, "browser-task >> CollectProfileJob >> zipProfiles >> logId: {0} >> response: {1}", new Object[]{logId, JsonConverter.convertObjectToJson(response)});
        CommonUtil.deleteFolder(new File(zipFileUrl));
        var url = uploadFileToGitHub(email, logId, baseDirectoryZipPath, response.getTotalPart());
        if (StringUtils.isNoneBlank(url)) {
            var clone = SerializationUtils.clone(item);
            clone.setProfileFolderUrl(url);
            clone.setUpdateDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            profileManagerRepo.saveProfileItem(clone);
        }
    }

    public String uploadFileToGitHub(String email, String logId, String folderUpLoad, int totalFile) {
        var config = new GitHubConfig(this.githubApiUrl, this.githubToken.trim());
        var allFileUnderFolder = GitHubService.getAllFileUnderFolder(email, config);
        if (CollectionUtils.isNotEmpty(allFileUnderFolder)) {
            allFileUnderFolder.forEach(it -> GitHubService.deleteFile(it.getSha(), it.getPath(), config));
        }
        var newFileUnder = GitHubService.getAllFileUnderFolder(email, config);
        if (CollectionUtils.isNotEmpty(newFileUnder)) {
            return email;
        }
        var directory = new File(folderUpLoad);
        var result = Arrays.stream(Optional.ofNullable(directory.listFiles()).orElse(new File[]{}))
                .map(it -> {
                    var path = MessageFormat.format("{0}/{1}", email, it.getName());
                    return GitHubService.uploadFileToGitHub(it, path, config);
                }).toList();
        var totalFilePush = GitHubService.getAllFileUnderFolder(email, config).size();
        if (totalFilePush == totalFile) {
            log.log(Level.INFO, "browser-task >> uploadFileToGitHub >> logId: {0} >> result >> {1} >> totalFileFromGit: {2}", new Object[]{logId, JsonConverter.convertObjectToJson(result), totalFile});
            return GitHubService.getAllFileUnderFolder("", config)
                    .stream().filter(it -> StringUtils.equalsIgnoreCase(it.getName(), email))
                    .map(ContentResponse::getUrl)
                    .findFirst().orElse("");
        }
        return "";
    }


}
