package com.task.service;

import com.task.common.CommonUtil;
import com.task.common.HttpUtil;
import com.task.common.JsonConverter;
import com.task.model.ActionStep;
import com.task.model.BrowserTask;
import com.task.model.ProfileItem;
import com.task.model.TaskResult;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Service
@Log
public class ChromeService {


    final FirebaseService firebaseService;

    @Value("${system.id}")
    private String headerKey;

    @Value("${system.profile-table-url}")
    private String profileTableUrl;

    public ChromeService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    public TaskResult handleBrowserTask(BrowserTask data) {
        return accessGoogle(data);
    }

    public TaskResult accessGoogle(BrowserTask task) {
        var folderName = UUID.randomUUID().toString();
        var result = new TaskResult(task, null);
        var cloneResult = SerializationUtils.clone(task);
        if (task.canNotStartBrowser()) {
            return result;
        }
        var url = "https://accounts.google.com";
        var options = createProfile(folderName, new ChromeOptions());
        var driver = new ChromeDriver(options);
        var email = "";
        try {

            driver.get(url);
            Thread.sleep(Duration.ofSeconds(5));
            driver.manage().window().maximize();
            System.setProperty("java.awt.headless", "false");
            var robot = new Robot();
            robot.keyPress(KeyEvent.VK_F11);

            preventCloseTab(driver);

            // check app can connect to login page by track the id identifierId
            Thread.sleep(Duration.ofSeconds(5).toMillis());
            var loginFormStatus = canConnectLoginPage(driver) ? ActionStep.CONNECTED_LOGIN_FORM.name() : ActionStep.CAN_NOT_FIND_LOGIN_FORM.name();
            firebaseService.updateTaskStatus(task.getTaskId(), loginFormStatus);
            Thread.sleep(Duration.ofSeconds(10).toMillis());
            var isSignedIn = loginSuccess(driver);

            if (isSignedIn) {
                log.log(Level.INFO, "browser-task >> ChromeService >> accessGoogle >> Google account is already logged in.");
                email = StringUtils.defaultIfBlank(findEmail(driver), "").trim();
                if (StringUtils.isBlank(email)) {
                    throw new IllegalArgumentException("can not extract email");
                }
                log.log(Level.INFO, "browser-task >> ChromeService >> accessGoogle >> email: {0}", email);
                firebaseService.updateTaskStatus(task.getTaskId(), ActionStep.LOGIN_SUCCESS.name());
                cloneResult.setProcessStep(ActionStep.LOGIN_SUCCESS.name());
            } else {
                log.log(Level.SEVERE, "browser-task >> ChromeService >> accessGoogle >> No Google account is logged in.");
                cloneResult.setProcessStep(ActionStep.LOGIN_FAILURE.name());
                firebaseService.updateTaskStatus(task.getTaskId(), ActionStep.LOGIN_FAILURE.name());
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "browser-task >> ChromeService >> accessGoogle >> Exception:", e);
            cloneResult.setProcessStep(ActionStep.LOGIN_FAILURE.name());
            firebaseService.updateTaskStatus(task.getTaskId(), ActionStep.LOGIN_FAILURE.name());
        } finally {
            driver.quit();
            // we can only rename the profile folder when we already closed the browser.
            if (cloneResult.getProcessStep().equalsIgnoreCase(ActionStep.LOGIN_SUCCESS.name()) && StringUtils.isNoneBlank(email)) {
                CommonUtil.deleteFolderByName(email);
                CommonUtil.renameFolder(folderName, email);
                saveProfileDatabase(email);
                firebaseService.updateTaskStatus(task.getTaskId(), ActionStep.UPDATED_THE_PROFILE_FOLDER.name());
            } else {
                CommonUtil.deleteFolderByName(folderName);
            }
        }
        result.setResponse(cloneResult);
        return result;
    }

    private boolean canConnectLoginPage(ChromeDriver driver) {
        var checkLoginForm = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    driver.findElement(By.id("identifierId"));
                    return true; // Profile icon found, already signed in
                } catch (NoSuchElementException e) {
                    return false; // Profile icon not found, not signed in
                }
            }
        };
        var wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofMinutes(5))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class);
        return wait.until(checkLoginForm);
    }


    private boolean loginSuccess(ChromeDriver driver) {
        var wait = new FluentWait<WebDriver>(driver)
                .withTimeout(Duration.ofMinutes(5))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class);


        // Define the condition to check for the profile icon
        var checkLogin = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                try {
                    driver.findElement(By.xpath("//a[contains(@href, 'accounts.google.com/SignOutOptions')]"));
                    return true; // Profile icon found, already signed in
                } catch (NoSuchElementException e) {
                    return false; // Profile icon not found, not signed in
                }
            }
        };
        return wait.until(checkLogin);
    }

    public void preventCloseTab(ChromeDriver driver) {
        var script = """
                    document.addEventListener('keydown', function(event) {
                        // Prevent opening a new tab with Ctrl+T or Ctrl+N
                        if ((event.ctrlKey && (event.key === 't' || event.key === 'n')) ||
                            // Prevent closing the current tab with Ctrl+W or Ctrl+F4
                            (event.ctrlKey && (event.key === 'w' || event.key === 'F4')) ||
                            // Prevent opening a new window with Ctrl+N
                            (event.ctrlKey && event.key === 'N') ||
                            // Prevent opening a new incognito window with Ctrl+Shift+N
                            (event.ctrlKey && event.shiftKey && event.key === 'N') ||
                            // Prevent closing the window with Alt+F4
                            (event.altKey && event.key === 'F4') ||
                            // Prevent switching between windows with Alt+Tab
                            (event.altKey && event.key === 'Tab') ||
                            // Prevent opening the system menu with F10
                            (event.key === 'F10') ||
                            // Prevent opening the context menu with Shift+F10
                            (event.shiftKey && event.key === 'F10') ||
                            // Prevent closing or minimizing the window with Ctrl+F4
                            (event.ctrlKey && event.key === 'F4') ||
                            // Prevent reopening the most recently closed tab with Ctrl+Shift+T
                            (event.ctrlKey && event.shiftKey && event.key === 'T')) {
                            event.preventDefault();
                        }
                    });
                """;
        var jsExecutor = (JavascriptExecutor) driver;
        jsExecutor.executeScript(script);
    }

    public ChromeOptions createProfile(String folderName, ChromeOptions options) {
        try {
            var profilePath = Paths.get(System.getProperty("user.home"), "chrome-profiles", folderName).toString();
            options.addArguments(MessageFormat.format("user-data-dir={0}", profilePath));
            options.addArguments("--disable-web-security");
            // this option so important to bypass google detection
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);
            options.addArguments("--disable-blink-features=AutomationControlled");

            return options;
        } catch (Exception e) {
            log.log(Level.WARNING, "browser-task >> ChromeService >> createProfile >> Exception:", e);
        }
        return options;
    }


    public String findEmail(ChromeDriver driver) {
        var input = driver.findElement(By.xpath("//a[contains(@href, 'accounts.google.com/SignOutOptions')]")).getAttribute("aria-label");
        var emailPattern = "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})";
        var pattern = Pattern.compile(emailPattern);
        var matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }


    void saveProfileDatabase(String email) {
        var header = HttpUtil.getHeaderPostRequest();
        header.add("realm", headerKey);
        var profile = ProfileItem.createOfflineProfile(email);
        var json = JsonConverter.convertObjectToJson(profile);
        var response = HttpUtil.sendPostRequest(profileTableUrl, json, header);
        var body = response.getBody();
        log.log(Level.INFO, "browser-task >> saveProfileDatabase >> header: {0} >> json: {1} >> url: {2} >> response: {3}",
                new Object[]{headerKey, json, profileTableUrl, body});

    }


}
