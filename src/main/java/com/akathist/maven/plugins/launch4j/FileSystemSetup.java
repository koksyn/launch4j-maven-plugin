package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

    /**
     * TODO: refactoring + tests
     * Unzips the given artifact in-place and returns the newly-unzipped top-level directory.
     * Writes a marker file to prevent unzipping more than once.
     */
    File unpackWorkDir(Artifact localArtifact) throws MojoExecutionException {
        File packedArtifactFile = findArtifactFileInsideJar(localArtifact);
        String packedArtifactFileName = packedArtifactFile.getName();
        String packedArtifactFileNameWithoutZipExtension = packedArtifactFileName.substring(0, packedArtifactFileName.length() - 4);

        File topLevelDirectory = packedArtifactFile.getParentFile();
        File unpackedMarkerFile = new File(topLevelDirectory, packedArtifactFileNameWithoutZipExtension + ".unpacked");
        File unpackedArtifactDir = new File(topLevelDirectory, packedArtifactFileNameWithoutZipExtension);

        // If the artifact is a SNAPSHOT, then a.getVersion() will report the long timestamp,
        // but getFile() will be 1.1-SNAPSHOT.
        // Since getFile() doesn't use the timestamp, all timestamps wind up in the same place.
        // Therefore, we need to expand the jar every time, if the unpackedMarkerFile file is stale.
        if (markerFileExistsAndIsYounger(packedArtifactFile, unpackedMarkerFile)) {
            log.info("Platform-specific work directory already exists: " + unpackedArtifactDir.getAbsolutePath());
        } else {
            tryUnpackFileIntoADirectory(packedArtifactFile, topLevelDirectory);
            tryCreateMarkerFile(unpackedMarkerFile);
        }

        applyRequiredPermissionsOnDir(unpackedArtifactDir);

        return unpackedArtifactDir;
    }

    private File findArtifactFileInsideJar(Artifact artifact) {
        if (artifact == null || artifact.getFile() == null) {
            throw new IllegalArgumentException("Cannot obtain file path to " + artifact);
        }

        File artifactFile = artifact.getFile();
        log.debug("Unpacking " + artifact + " into " + artifactFile);
        return artifactFile;
    }

    private static boolean markerFileExistsAndIsYounger(File packedArtifactFile, File unpackedMarkerFile) {
        return unpackedMarkerFile.exists() && unpackedMarkerFile.lastModified() > packedArtifactFile.lastModified();
    }

    private static void tryUnpackFileIntoADirectory(File packedFile, File topLevelDirectory) throws MojoExecutionException {
        // trying to use plexus-archiver here is a miserable waste of time:
        try (JarFile jar = new JarFile(packedFile)) {
            Enumeration<JarEntry> jarEntries = jar.entries();

            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();

                File unpackedEntry = new File(topLevelDirectory, jarEntry.getName());
                if (!unpackedEntry.toPath().normalize().startsWith(topLevelDirectory.toPath().normalize())) {
                    throw new RuntimeException("Bad zip entry");
                }
                File unpackedEntryParentDir = unpackedEntry.getParentFile();
                if (unpackedEntryParentDir != null) { // why mkdir, when it is not null? and possible exists?
                    unpackedEntryParentDir.mkdirs();
                }

                if (jarEntry.isDirectory()) {
                    unpackedEntry.mkdirs();
                } else {
                    copyFileFromJar(jar, jarEntry, unpackedEntry);
                    unpackedEntry.setLastModified(jarEntry.getTime());
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error unarchiving " + packedFile, e);
        }
    }

    private static void copyFileFromJar(JarFile jar, JarEntry jarEntry, File unpackedEntry) throws IOException {
        try (InputStream jarEntryInputStream = jar.getInputStream(jarEntry);
             FileOutputStream unpackedFileOutputStream = new FileOutputStream(unpackedEntry)) {
            rewriteBytes(jarEntryInputStream, unpackedFileOutputStream);
        }
    }

    private static void rewriteBytes(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) >= 0) {
            fileOutputStream.write(buf, 0, len);
        }
    }

    private void tryCreateMarkerFile(File unpackedMarkerFile) {
        try {
            unpackedMarkerFile.createNewFile();
            unpackedMarkerFile.setLastModified(new Date().getTime());
        } catch (IOException e) {
            log.warn("Trouble creating unpackedMarkerFile file " + unpackedMarkerFile, e);
        }
    }

    /**
     * TODO: refactoring + tests
     * Chmods the helper executables ld and windres on systems where that is necessary.
     */
    private void applyRequiredPermissionsOnDir(File unpackedArtifactDir) {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            try {
                new ProcessBuilder("chmod", "755", unpackedArtifactDir + "/bin/ld").start().waitFor();
                new ProcessBuilder("chmod", "755", unpackedArtifactDir + "/bin/windres").start().waitFor();
            } catch (InterruptedException e) {
                log.warn("Interrupted while chmodding platform-specific binaries", e);
            } catch (IOException e) {
                log.warn("Unable to set platform-specific binaries to 755", e);
            }
        }
    }
}
