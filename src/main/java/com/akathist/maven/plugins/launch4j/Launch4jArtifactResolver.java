package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;

class Launch4jArtifactResolver {
    static final String LAUNCH4J_ARTIFACT_ID = "launch4j";

    static final String LAUNCH4J_GROUP_ID = "net.sf.launch4j";

    private final Log log;
    private final ArtifactResolver artifactResolver;
    private final RepositorySystem repositorySystemFactory;
    private final ArtifactVersionDetector artifactVersionDetector;
    private final PlatformDetector platformDetector;

    public Launch4jArtifactResolver(Log log, ArtifactResolver artifactResolver, RepositorySystem repositorySystemFactory, ArtifactVersionDetector artifactVersionDetector, PlatformDetector platformDetector) {
        this.log = log;
        this.artifactResolver = artifactResolver;
        this.repositorySystemFactory = repositorySystemFactory;
        this.artifactVersionDetector = artifactVersionDetector;
        this.platformDetector = platformDetector;
    }

    Artifact resolveArtifact(ProjectBuildingRequest configuration) {
        if(configuration == null) {
            throw new IllegalArgumentException("configuration is null.");
        }

        String osPlatform = platformDetector.detectOSFromSystemProperties();
        String launch4jVersion = tryDetectLaunch4jVersion();

        Artifact launch4jArtifactDefinition = getPlatformSpecificArtifactDefinition(osPlatform, launch4jVersion);
        resolveArtifact(configuration, launch4jArtifactDefinition);

        return launch4jArtifactDefinition;
    }

    /**
     * A version of the Launch4j used by the plugin.
     * We want to download the platform-specific bundle whose version matches the Launch4j version,
     * so we have to figure out what version the plugin is using.
     *
     * @return version of Launch4j
     * @throws IllegalStateException when cannot find a version
     */
    private String tryDetectLaunch4jVersion() {
        try {
            return artifactVersionDetector.detect(LAUNCH4J_ARTIFACT_ID, LAUNCH4J_GROUP_ID);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Impossible to find which Launch4j version to use", exception);
        }
    }

    /**
     * Decides which platform-specific bundle we need, based on the current operating system.
     */
    private Artifact getPlatformSpecificArtifactDefinition(String osPlatform, String launch4jVersion) {
        return repositorySystemFactory.createArtifactWithClassifier(
                LAUNCH4J_GROUP_ID,
                LAUNCH4J_ARTIFACT_ID,
                launch4jVersion,
                "jar",
                "workdir-" + osPlatform
        );
    }

    /**
     * Downloads the platform-specific parts, if necessary.
     */
    private void resolveArtifact(ProjectBuildingRequest configuration, Artifact artifact) {
        log.debug("Retrieving artifact: " + artifact + " stored in " + artifact.getFile());

        try {
            artifactResolver.resolveArtifact(configuration, artifact).getArtifact();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Illegal Argument Exception", e);
        } catch (ArtifactResolverException e) {
            throw new IllegalStateException("Can't retrieve platform-specific components", e);
        }
    }
}
