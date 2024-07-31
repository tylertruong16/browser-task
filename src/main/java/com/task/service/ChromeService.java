package com.task.service;

import com.task.common.CommonUtil;
import com.task.model.ActionStep;
import com.task.model.BrowserTask;
import com.task.model.TaskResult;
import lombok.extern.java.Log;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Service
@Log
public class ChromeService {

    public TaskResult handleBrowserTask(BrowserTask data) {
        return accessGoogle(data);
    }

    public TaskResult accessGoogle(BrowserTask task) {
        var folderName = UUID.randomUUID().toString();
        var result = new TaskResult(task, null);
        var cloneResult = SerializationUtils.clone(task);
        var stepHistory = new ArrayList<String>();
        if (task.canNotStartBrowser()) {
            return result;
        }
        stepHistory.add(ActionStep.CONNECT_GOOGLE.name());
        var url = "https://www.google.com";
        var options = createProfile(folderName, new ChromeOptions());
        var driver = new ChromeDriver(options);
        try {

            driver.manage().window().maximize();
            driver.get(url);
            Thread.sleep(Duration.ofSeconds(10).toMillis());
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
            boolean isSignedIn = wait.until(checkLogin);

            if (isSignedIn) {
                log.log(Level.INFO, "browser-task >> ChromeService >> accessGoogle >> Google account is already logged in.");
                var email = StringUtils.defaultIfBlank(findEmail(driver), "").trim();
                if (StringUtils.isBlank(email)) {
                    throw new IllegalArgumentException("can not extract email");
                }
                log.log(Level.INFO, "browser-task >> ChromeService >> accessGoogle >> email: {0}", email);
                CommonUtil.renameFolder(folderName, email);
                cloneResult.setProcessStep(ActionStep.LOGIN_SUCCESS.name());
                stepHistory.add(ActionStep.LOGIN_SUCCESS.name());
            } else {
                log.log(Level.SEVERE, "browser-task >> ChromeService >> accessGoogle >> No Google account is logged in.");
                cloneResult.setProcessStep(ActionStep.LOGIN_FAILURE.name());
                stepHistory.add(ActionStep.LOGIN_FAILURE.name());
                CommonUtil.deleteFolderByName(folderName);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "browser-task >> ChromeService >> accessGoogle >> Exception:", e);
            cloneResult.setProcessStep(ActionStep.LOGIN_FAILURE.name());
            stepHistory.add(ActionStep.LOGIN_FAILURE.name());
            CommonUtil.deleteFolderByName(folderName);
        } finally {
            driver.quit();
        }
        cloneResult.setStepHistory(stepHistory);
        result.setResponse(cloneResult);
        return result;
    }

    public ChromeOptions createProfile(String folderName, ChromeOptions options) {
        try {
            var profilePath = Paths.get(System.getProperty("user.home"), "chrome-profiles", folderName).toString();
            options.addArguments(MessageFormat.format("user-data-dir={0}", profilePath));
            options.addArguments("--disable-web-security");
            // this option so important to bypass google detection
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);
            options.addArguments("disable-infobars");
            options.addArguments("--start-fullscreen");
            options.setExperimentalOption("useAutomationExtension", false);

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


}
