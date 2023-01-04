package com.akathist.maven.plugins.launch4j;

import org.apache.maven.plugin.logging.Log;

import java.io.File;

class FileSystemSetup {
    private final Log log;

    public FileSystemSetup(Log log) {
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
}
