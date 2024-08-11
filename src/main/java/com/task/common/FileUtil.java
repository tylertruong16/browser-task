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

@UtilityClass
@Log
public class FileUtil {

    public static void zipFolder(String sourceFolder, String zipFile) throws IOException {
        File zipFilePath = new File(zipFile);
        File parentDir = zipFilePath.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                log.log(Level.WARNING, "browser-task >> FileUtil >> zipFolder >> Created missing directories: {0}", parentDir.getAbsolutePath());
            } else {
                log.log(Level.WARNING, "browser-task >> FileUtil >> zipFolder >> Failed to create directories: {0}", parentDir.getAbsolutePath());
                return;
            }
        }
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos);

        File folder = new File(sourceFolder);
        zipFiles(folder, folder.getName(), zos);

        zos.finish();
        zos.close();
        fos.close();
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
