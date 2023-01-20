package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

class ArtifactVersionDetector {
    private final List<Artifact> pluginArtifacts;
    private final Log log;

    ArtifactVersionDetector(List<Artifact> pluginArtifacts, Log log) {
        this.pluginArtifacts = pluginArtifacts;
        this.log = log;
    }

    String detect(String artifactId, String groupId) {
        if(artifactId == null) {
            throw new IllegalArgumentException("artifactId cannot be null");
        }
        if(groupId == null) {
            throw new IllegalArgumentException("groupId cannot be null");
        }

        String version = null;

        for (Artifact artifact : pluginArtifacts) {
            if (groupId.equals(artifact.getGroupId()) &&
                    artifactId.equals(artifact.getArtifactId())
                    && "core".equals(artifact.getClassifier())) {
                version = artifact.getVersion();
                log.debug("Found " + artifactId + " version " + version);
                break;
            }
        }

        if (version == null) {
            throw new IllegalStateException("Impossible to find artifact " + artifactId + " version");
        }

        return version;
    }
}
