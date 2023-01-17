package com.akathist.maven.plugins.launch4j;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

class FileSystemUtil {
    private final Log log;

    public FileSystemUtil(Log log) {
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
}
