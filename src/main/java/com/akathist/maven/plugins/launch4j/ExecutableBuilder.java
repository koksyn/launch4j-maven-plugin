package com.akathist.maven.plugins.launch4j;

import net.sf.launch4j.Builder;
import net.sf.launch4j.BuilderException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;

class ExecutableBuilder {
    private final Log log;
    private final MavenLog mavenLog;
    private final ExecutableBuilderFactory executableBuilderFactory;

    ExecutableBuilder(Log log, ExecutableBuilderFactory executableBuilderFactory) {
        this.log = log;
        this.mavenLog = new MavenLog(log);
        this.executableBuilderFactory = executableBuilderFactory;
    }

    void build(File baseDirectory) {
        if(baseDirectory == null) {
            throw new IllegalArgumentException("Base directory is null.");
        }
        if(!baseDirectory.exists()) {
            throw new IllegalArgumentException("Base directory under path: '" + baseDirectory.getPath() + "' does not exist.");
        }

        final Builder builder = executableBuilderFactory.build(mavenLog, baseDirectory);

        try {
            builder.build();
        } catch (BuilderException exception) {
            log.error(exception);
            throw new IllegalStateException("Cannot build executable. Please verify your configuration.", exception);
        }
    }
}
