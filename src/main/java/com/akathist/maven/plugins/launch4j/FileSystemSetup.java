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
     * Unzips the given artifact in-place and returns the newly-unzipped top-level directory.
     * Writes a marker file to prevent unzipping more than once.
     */
    File unpackWorkDir(Artifact localArtifact) throws MojoExecutionException {
        if (localArtifact == null || localArtifact.getFile() == null) {
            throw new MojoExecutionException("Cannot obtain file path to " + localArtifact);
        }
        log.debug("Unpacking " + localArtifact + " into " + localArtifact.getFile());
        File platJar = localArtifact.getFile();
        File dest = platJar.getParentFile();
        File marker = new File(dest, platJar.getName() + ".unpacked");
        String n = platJar.getName();
        File workdir = new File(dest, n.substring(0, n.length() - 4));

        // If the artifact is a SNAPSHOT, then a.getVersion() will report the long timestamp,
        // but getFile() will be 1.1-SNAPSHOT.
        // Since getFile() doesn't use the timestamp, all timestamps wind up in the same place.
        // Therefore, we need to expand the jar every time, if the marker file is stale.
        if (marker.exists() && marker.lastModified() > platJar.lastModified()) {
            // if (marker.exists() && marker.platJar.getName().indexOf("SNAPSHOT") == -1) {
            log.info("Platform-specific work directory already exists: " + workdir.getAbsolutePath());
        } else {
            // trying to use plexus-archiver here is a miserable waste of time:
            try (JarFile jf = new JarFile(platJar)) {
                Enumeration<JarEntry> en = jf.entries();
                while (en.hasMoreElements()) {
                    JarEntry je = en.nextElement();
                    File outFile = new File(dest, je.getName());
                    if (!outFile.toPath().normalize().startsWith(dest.toPath().normalize())) {
                        throw new RuntimeException("Bad zip entry");
                    }
                    File parent = outFile.getParentFile();
                    if (parent != null) parent.mkdirs();
                    if (je.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        try (InputStream in = jf.getInputStream(je)) {
                            try (FileOutputStream fout = new FileOutputStream(outFile)) {
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = in.read(buf)) >= 0) {
                                    fout.write(buf, 0, len);
                                }
                            }
                        }
                        outFile.setLastModified(je.getTime());
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Error unarchiving " + platJar, e);
            }

            try {
                marker.createNewFile();
                marker.setLastModified(new Date().getTime());
            } catch (IOException e) {
                log.warn("Trouble creating marker file " + marker, e);
            }
        }

        setPermissions(workdir);
        return workdir;
    }

    /**
     * Chmods the helper executables ld and windres on systems where that is necessary.
     */
    private void setPermissions(File workdir) {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            try {
                new ProcessBuilder("chmod", "755", workdir + "/bin/ld").start().waitFor();
                new ProcessBuilder("chmod", "755", workdir + "/bin/windres").start().waitFor();
            } catch (InterruptedException e) {
                log.warn("Interrupted while chmodding platform-specific binaries", e);
            } catch (IOException e) {
                log.warn("Unable to set platform-specific binaries to 755", e);
            }
        }
    }
}
