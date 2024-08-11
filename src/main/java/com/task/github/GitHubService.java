package com.task.github;

import com.task.common.HttpUtil;
import com.task.common.JsonConverter;
import com.task.github.model.ContentResponse;
import com.task.github.model.GitHubConfig;
import com.task.github.model.UpLoadResponse;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

@UtilityClass
@Log
public class GitHubService {


    @SneakyThrows
    public static UpLoadResponse uploadFileToGitHub(File file, String remotePath, GitHubConfig gitHubConfig) {
        var fileContentBase64 = encodeFileToBase64(file);
        var body = getDefaultBody();
        body.put("message", "add: " + file.getName());
        body.put("content", fileContentBase64);
        var url = gitHubConfig.getApiUrl() + "/" + remotePath;
        var header = getHttpHeaders(gitHubConfig.getApiKey());
        var request = new HttpPut(url);
        var entity = new StringEntity(JsonConverter.convertObjectToJson(body), ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        var response = HttpUtil.sendRequest(url, request, header);
        var responseBody = response.getBody();
        log.log(Level.INFO, "browser-task >> GitHubService >> uploadFileToGitHub >> responseBody: {0}", responseBody);
        var status = response.getStatusCode().is2xxSuccessful();
        return UpLoadResponse.builder()
                .status(status)
                .fileName(file.getName()).build();
    }

    private static HttpHeaders getHttpHeaders(String token) {
        var header = HttpUtil.getHeaderPostRequest();
        header.add("Authorization", "Bearer " + token);
        header.add("Content-Type", "application/vnd.github+json");
        header.add("X-GitHub-Api-Version", "2022-11-28");
        return header;
    }

    public boolean deleteFile(String sha, String path, GitHubConfig gitHubConfig) {
        var body = getDefaultBody();
        body.put("message", "delete file with sha: " + sha);
        body.put("sha", sha);
        var url = MessageFormat.format("{0}/{1}", gitHubConfig.getApiUrl(), path);
        var header = getHttpHeaders(gitHubConfig.getApiKey());
        var request = new HttpDelete(url);
        var entity = new StringEntity(JsonConverter.convertObjectToJson(body), ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        var response = HttpUtil.sendRequest(url, request, header);
        log.log(Level.INFO, "browser-task >> GitHubService >> deleteFile: {0}", response.getBody());
        return response.getStatusCode().is2xxSuccessful();
    }


    public List<ContentResponse> getAllFileUnderFolder(String folderName, GitHubConfig gitHubConfig) {
        var url = MessageFormat.format("{0}/{1}", gitHubConfig.getApiUrl(), folderName);
        var header = getHttpHeaders(gitHubConfig.getApiKey());
        var request = new HttpGet(url);
        var response = HttpUtil.sendRequest(url, request, header);
        if (response.getStatusCode().value() == 404) {
            return new ArrayList<>();
        }
        var responseBody = response.getBody();
        return Arrays.stream(JsonConverter.convertToObject(responseBody, ContentResponse[].class).orElse(new ContentResponse[]{})).toList();
    }


    @SneakyThrows
    private static String encodeFileToBase64(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] fileContent = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(fileContent);
        }
    }

    public Map<String, Object> getDefaultBody() {
        var body = new HashMap<String, Object>();
        body.put("committer", Map.of("name", "Monalisa Octocat", "email", "octocat@github.com"));
        return body;
    }


}
