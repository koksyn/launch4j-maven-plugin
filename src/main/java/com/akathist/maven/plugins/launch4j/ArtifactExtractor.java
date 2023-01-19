package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;

class ArtifactExtractor {
    private final JarFileExtractor jarFileExtractor;
    private final FileSystemUtil fileSystemUtil;
    private final Log log;

    ArtifactExtractor(JarFileExtractor jarFileExtractor, FileSystemUtil fileSystemUtil, Log log) {
        this.jarFileExtractor = jarFileExtractor;
        this.fileSystemUtil = fileSystemUtil;
        this.log = log;
    }

    /**
     * Unpacks the given artifact in-place (from Jar) and returns the newly-unzipped top-level directory.
     * Writes a marker file to prevent unzipping more than once.
     */
    File unpackAndGetUnpackedDir(Artifact artifact) {
        File packedArtifactFile = findArtifactFile(artifact);
        log.debug("Unpacking " + artifact + " from " + packedArtifactFile);
        String packedArtifactFileNameWithoutExtension = fileSystemUtil.retrieveFileNameWithoutArchiveExtension(packedArtifactFile);

        File topLevelDirectory = packedArtifactFile.getParentFile();
        File marker = new File(topLevelDirectory, packedArtifactFileNameWithoutExtension + ".unpacked");
        File unpackedDirectory = new File(topLevelDirectory, packedArtifactFileNameWithoutExtension);

        // If the artifact is a SNAPSHOT, then artifact.getVersion() will report the long timestamp, but getFile() will be 1.1-SNAPSHOT.
        // Since getFile() doesn't use the timestamp, all timestamps wind up in the same place.
        // Therefore, we need to expand the jar every time, if the marker file is stale.
        if (fileSystemUtil.fileExistsAndIsYoungerThan(marker, packedArtifactFile.lastModified())) {
            log.info("Platform-specific work directory already exists: " + unpackedDirectory.getAbsolutePath());
        } else {
            fileSystemUtil.deleteFileQuietly(marker);
            jarFileExtractor.tryUnpackIntoDir(packedArtifactFile, topLevelDirectory);
            fileSystemUtil.createFileQuietly(marker);
        }

        if (!System.getProperty("os.name").startsWith("Windows")) {
            setPermissionsForNonWindowsBinaries(unpackedDirectory);
        }

        return unpackedDirectory;
    }

    private File findArtifactFile(Artifact artifact) {
        if (artifact == null || artifact.getFile() == null) {
            throw new IllegalArgumentException("Cannot obtain file path to " + artifact);
        }

        return artifact.getFile();
    }

    /**
     * Chmods the helper executables ld and windres on systems where that is necessary.
     */
    private void setPermissionsForNonWindowsBinaries(File sourceDir) {
        try {
            new ProcessBuilder("chmod", "755", sourceDir + "/bin/ld").start().waitFor();
            new ProcessBuilder("chmod", "755", sourceDir + "/bin/windres").start().waitFor();
        } catch (InterruptedException e) {
            log.warn("Interrupted while chmodding platform-specific binaries", e);
        } catch (IOException e) {
            log.warn("Unable to set platform-specific binaries to 755", e);
        }
    }
}
