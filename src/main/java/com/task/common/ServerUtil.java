package com.task.common;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

@UtilityClass
@Log
public class ServerUtil {

    public String getServerIP() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream().map(NetworkInterface::getInetAddresses).map(Collections::list).flatMap(List::stream)
                    .filter(ip -> ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && !ip.isLinkLocalAddress() && !ip.getHostAddress().contains(":"))
                    .findFirst().map(InetAddress::getHostAddress).orElseThrow(() -> new RuntimeException("Cannot determine server IP address"));
        } catch (IOException ex) {
            log.log(Level.SEVERE, "browser-task >> ServerUtil >> getServerIP >> Cannot determine server IP address", ex);
            return "";
        }
    }

    public static List<String> getAllFolderNames() {
        var userHome = System.getProperty("user.home");
        var chromeProfilesPath = MessageFormat.format("{0}/{1}", userHome, "chrome-profiles");
        var chromeProfilesDir = new File(chromeProfilesPath);
        if (chromeProfilesDir.exists() && chromeProfilesDir.isDirectory()) {
            return Arrays.stream(Objects.requireNonNull(chromeProfilesDir.listFiles()))
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
