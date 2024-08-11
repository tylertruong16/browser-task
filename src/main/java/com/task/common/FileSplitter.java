package com.task.common;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.ToIntBiFunction;

@UtilityClass
@Log
public class FileSplitter {

    private static final String KEY = "03TnhH9pyFnOJhYhVxqnwl5JsiVfIl3N";

    @Data
    public static class FileZipDetail {
        private int totalPart;
        private String md5CheckSum;
    }

    @SneakyThrows
    public static FileZipDetail splitFile(String sourceFilePath, int partSizeMB) {
        var sourceFile = new File(sourceFilePath);
        long fileSize = sourceFile.length();
        long partSize = (long) partSizeMB * 1024 * 1024;

        int partCount = (int) (fileSize / partSize) + (fileSize % partSize == 0 ? 0 : 1);
        var fileZipDetail = new FileZipDetail();
        fileZipDetail.setTotalPart(partCount);
        var md5Digest = MessageDigest.getInstance("MD5");
        var folderPath = sourceFile.getParent();
        var fileName = sourceFile.getName();

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile))) {
            for (int i = 0; i < partCount; i++) {
                var newFileName = Base64.encodeBase64URLSafeString(AESUtil.encrypt(fileName + ".part" + i, KEY).getBytes());
                var partFileName = folderPath + File.separator + newFileName;
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(partFileName))) {
                    long bytesWritten = 0;
                    byte[] buffer = new byte[8192];
                    while (bytesWritten < partSize && bis.available() > 0) {
                        int bytesRead = bis.read(buffer);
                        bos.write(buffer, 0, bytesRead);
                        bytesWritten += bytesRead;

                        // Update MD5 checksum
                        md5Digest.update(buffer, 0, bytesRead);
                    }
                }
            }
        }
        // Set the MD5 checksum in the fileZipDetail object
        byte[] md5Bytes = md5Digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : md5Bytes) {
            sb.append(String.format("%02x", b));
        }
        fileZipDetail.setMd5CheckSum(sb.toString());
        return fileZipDetail;
    }

    @SneakyThrows
    public static void mergeFiles(String partFilePath, String destinationFilePath) {
        var partFileFolder = new File(partFilePath);
        var directory = partFileFolder.isDirectory() ? partFileFolder : partFileFolder.getParentFile();
        var fileName = new File(destinationFilePath).getName();
        var zipHint = fileName + ".part";
        Comparator<File> nameComparator = (h1, h2) -> {
            ToIntBiFunction<File, String> getPartNumber = (File f, String hint) -> NumberUtils.toInt(f.getName().replace(hint, ""), 0);
            var h1Number = getPartNumber.applyAsInt(h1, zipHint);
            var h2Number = getPartNumber.applyAsInt(h2, zipHint);
            return h1Number - h2Number;
        };
        var arrayFiles = directory.listFiles((_, name) -> {
            if (!Base64.isBase64(name)) {
                return false;
            }
            var decode = new String(Base64.decodeBase64(name), StandardCharsets.UTF_8);
            var oldFileName = AESUtil.decrypt(decode, KEY);
            return oldFileName.startsWith(fileName) && oldFileName.contains(zipHint);
        });
        var partFiles = Arrays.stream(Optional.ofNullable(arrayFiles).orElse(new File[]{}))
                .sorted(nameComparator).toArray(File[]::new);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destinationFilePath))) {
            for (File partFile : partFiles) {
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(partFile))) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

}
