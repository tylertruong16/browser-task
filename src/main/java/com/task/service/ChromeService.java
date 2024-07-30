package com.task.service;

import lombok.extern.java.Log;
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
import java.util.function.Function;
import java.util.logging.Level;

@Service
@Log
public class ChromeService {



    public void accessGoogle(String email) {
        var url = "https://www.google.com";
        var options = createProfile(email, new ChromeOptions());
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
            } else {
                log.log(Level.SEVERE, "browser-task >> ChromeService >> accessGoogle >> No Google account is logged in.");
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "browser-task >> ChromeService >> accessGoogle >> Exception:", e);
        } finally {
            driver.quit();
        }
    }

    public ChromeOptions createProfile(String email, ChromeOptions options) {
        try {
            var profilePath = Paths.get(System.getProperty("user.home"), "chrome-profiles", email).toString();
            options.addArguments(MessageFormat.format("user-data-dir={0}", profilePath));
            options.addArguments("--disable-web-security");
            // this option so important to bypass google detection
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.setExperimentalOption("useAutomationExtension", false);

            return options;
        } catch (Exception e) {
            log.log(Level.WARNING, "browser-task >> ChromeService >> createProfile >> Exception:", e);
        }
        return options;
    }

}
