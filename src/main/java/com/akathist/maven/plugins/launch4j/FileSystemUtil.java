package com.akathist.maven.plugins.launch4j;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

class FileSystemUtil {
    private final Log log;

    FileSystemUtil(Log log) {
        this.log = log;
    }

    void createParentFolderQuietly(File subject) {
        if (subject != null) {
            File parent = subject.getParentFile();

            if (!parent.exists()) {
                final String parentPath = parent.getPath();
                log.debug("Parent " + parentPath + " does not exist, creating it!");

                boolean parentDirectoryCreated = parent.mkdirs();
                if (parentDirectoryCreated) {
                    log.debug("Parent " + parentPath + " has been created!");
                } else {
                    log.warn("Cannot create parent " + parentPath + "!");
                }
            }
        }
    }

    void createFileQuietly(File file) {
        if(file != null) {
            try {
                file.createNewFile();
                file.setLastModified(new Date().getTime());
            } catch (IOException e) {
                log.warn("Trouble creating file " + file, e);
            }
        }
    }

    void deleteFileQuietly(File file) {
        if(file != null) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                log.warn("Trouble deleting file " + file, e);
            }
        }
    }

    boolean fileExistsAndIsYoungerThan(File file, long timestamp) {
        return file.exists() && file.lastModified() > timestamp;
    }

    String retrieveFileNameWithoutArchiveExtension(File file) {
        if(file == null) {
            throw new IllegalArgumentException("Cannot retrieve fileName without JAR/ZIP ext, because file is null.");
        }

        String fileName = file.getName();

        String fileNameLowercase = fileName.toLowerCase();
        if (fileNameLowercase.endsWith(".jar") || fileNameLowercase.endsWith(".zip")) {
            return fileName.substring(0, fileName.length() - 4);
        }

        return fileName;
    }

    void setNonWindowsFilePermissionsQuietly(String filePath, String permissions) {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            try {
                new ProcessBuilder("chmod", permissions, filePath).start().waitFor();
            } catch (InterruptedException e) {
                log.warn("Interrupted while chmodding file " + filePath, e);
            } catch (IOException e) {
                log.warn("Unable to set file " + filePath + " permissions to " + permissions, e);
            }
        }
    }
}
