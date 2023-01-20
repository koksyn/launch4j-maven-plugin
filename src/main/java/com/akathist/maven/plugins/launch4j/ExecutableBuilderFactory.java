package com.akathist.maven.plugins.launch4j;

import net.sf.launch4j.Builder;

import java.io.File;

class ExecutableBuilderFactory {
    Builder build(MavenLog mavenLog, File buildDirectory) {
        return new Builder(mavenLog, buildDirectory);
    }
}
