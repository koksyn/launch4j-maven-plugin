package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;

import java.util.List;

class Launch4jArtifactCreator {
    private static final String LAUNCH4J_ARTIFACT_ID = "launch4j";

    private static final String LAUNCH4J_GROUP_ID = "net.sf.launch4j";

    private final Log log;
    private final ArtifactResolver artifactResolver;
    private final RepositorySystem repositorySystemFactory;
    private final List<Artifact> pluginArtifacts;

    Launch4jArtifactCreator(Log log,
                                   ArtifactResolver artifactResolver,
                                   RepositorySystem repositorySystem,
                                   List<Artifact> pluginArtifacts) {
        this.log = log;
        this.artifactResolver = artifactResolver;
        this.repositorySystemFactory = repositorySystem;
        this.pluginArtifacts = pluginArtifacts;
    }
    
    /**
     * Decides which platform-specific bundle we need, based on the current operating system.
     */
    Artifact chooseBinaryBits() throws MojoExecutionException {
        String plat;
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        log.debug("OS = " + os);
        log.debug("Architecture = " + arch);

        // See here for possible values of os.name:
        // http://lopica.sourceforge.net/os.html
        if (os.startsWith("Windows")) {
            plat = "win32";
        } else if ("Linux".equals(os)) {
            if ("amd64".equals(arch)) {
                plat = "linux64";
            } else {
                plat = "linux";
            }
        } else if ("Solaris".equals(os) || "SunOS".equals(os)) {
            plat = "solaris";
        } else if ("Mac OS X".equals(os) || "Darwin".equals(os)) {
            plat = "mac";
        } else {
            throw new MojoExecutionException("Sorry, Launch4j doesn't support the '" + os + "' OS.");
        }

        return repositorySystemFactory.createArtifactWithClassifier(LAUNCH4J_GROUP_ID, LAUNCH4J_ARTIFACT_ID,
                getLaunch4jVersion(), "jar", "workdir-" + plat);
    }

    /**
     * Downloads the platform-specific parts, if necessary.
     */
    void retrieveBinaryBits(ProjectBuildingRequest configuration, Artifact a) throws MojoExecutionException {
        log.debug("Retrieving artifact: " + a + " stored in " + a.getFile());

        try {
            artifactResolver.resolveArtifact(configuration, a).getArtifact();
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Illegal Argument Exception", e);
        } catch (ArtifactResolverException e) {
            throw new MojoExecutionException("Can't retrieve platform-specific components", e);
        }
    }

    /**
     * A version of the Launch4j used by the plugin.
     * We want to download the platform-specific bundle whose version matches the Launch4j version,
     * so we have to figure out what version the plugin is using.
     *
     * @return version of Launch4j
     * @throws MojoExecutionException when version is null
     */
    private String getLaunch4jVersion() throws MojoExecutionException {
        String version = null;

        for (Artifact artifact : pluginArtifacts) {
            if (LAUNCH4J_GROUP_ID.equals(artifact.getGroupId()) &&
                    LAUNCH4J_ARTIFACT_ID.equals(artifact.getArtifactId())
                    && "core".equals(artifact.getClassifier())) {

                version = artifact.getVersion();
                log.debug("Found launch4j version " + version);
                break;
            }
        }

        if (version == null) {
            throw new MojoExecutionException("Impossible to find which Launch4j version to use");
        }

        return version;
    }
}
