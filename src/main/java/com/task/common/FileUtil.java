package com.task.common;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@UtilityClass
@Log
public class FileUtil {


    public static void zipFiles(String[] srcFiles, String zipFile) throws IOException {
        var zipFilePath = new File(zipFile);
        var parentDir = zipFilePath.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                log.log(Level.WARNING, "browser-task >> FileUtil >> zipFolder >> Created missing directories: {0}", parentDir.getAbsolutePath());
            } else {
                log.log(Level.WARNING, "browser-task >> FileUtil >> zipFolder >> Failed to create directories: {0}", parentDir.getAbsolutePath());
                return;
            }
        }
        try (var fos = new FileOutputStream(zipFile);
             var zos = new ZipOutputStream(fos)) {

            for (var srcFile : srcFiles) {
                var fileToZip = new File(srcFile);
                zipFile(fileToZip, fileToZip.getName(), zos);
            }
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            log.log(Level.WARNING, "browser-task >> FileUtil >> zipFolder >> folder hidden: {0}", fileToZip.getAbsolutePath());
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
                zos.closeEntry();
            }
            var children = fileToZip.listFiles();
            if (children != null) {
                for (var childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zos);
                }
            }
            return;
        }
        try (var fis = new FileInputStream(fileToZip)) {
            var zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
    }

    private static void zipFiles(File fileToZip, String parentFolder, ZipArchiveOutputStream zos) throws IOException {
        if (fileToZip.isDirectory()) {
            for (File file : Objects.requireNonNull(fileToZip.listFiles())) {
                zipFiles(file, parentFolder + "/" + file.getName(), zos);
            }
        } else {
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(fileToZip, parentFolder);
            zos.putArchiveEntry(zipEntry);
            IOUtils.copy(fis, zos);
            zos.closeArchiveEntry();
            fis.close();
        }
    }

}
