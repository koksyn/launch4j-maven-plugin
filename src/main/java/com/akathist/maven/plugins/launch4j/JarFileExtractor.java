package com.akathist.maven.plugins.launch4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class JarFileExtractor {
    private final FileSystemUtil fileSystemUtil;

    JarFileExtractor(FileSystemUtil fileSystemUtil) {
        this.fileSystemUtil = fileSystemUtil;
    }

    /**
     * trying to use plexus-archiver here is a miserable waste of time
     */
    void tryUnpackIntoDir(File fileInsideJar, File destinationDir) {
        if(fileInsideJar == null) {
            throw new IllegalArgumentException("fileInsideJar is null.");
        }
        if(destinationDir == null) {
            throw new IllegalArgumentException("destinationDir is null.");
        }

        try (JarFile jar = new JarFile(fileInsideJar)) {
            Enumeration<JarEntry> jarEntries = jar.entries();

            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                tryUnpackJarEntryIntoDir(jar, jarEntry, destinationDir);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Error unpacking " + fileInsideJar, exception);
        }
    }

    private void tryUnpackJarEntryIntoDir(JarFile jar, JarEntry jarEntry, File destinationDir) throws IOException {
        File unpackedEntry = new File(destinationDir, jarEntry.getName());
        if (fileSystemUtil.fileLocatedOutsideDir(unpackedEntry, destinationDir)) {
            throw new IllegalStateException("Bad jar entry: '" + unpackedEntry.getAbsolutePath() + "'. " +
                    "Located outside destination directory: " + destinationDir.getAbsolutePath());
        }

        fileSystemUtil.createParentFolderQuietly(unpackedEntry);

        if (jarEntry.isDirectory()) {
            unpackedEntry.mkdirs();
        } else {
            extractFileFromJar(jar, jarEntry, unpackedEntry);
            unpackedEntry.setLastModified(jarEntry.getTime());
        }
    }

    private void extractFileFromJar(JarFile jar, JarEntry jarEntry, File destinationFile) throws IOException {
        try (InputStream jarEntryInputStream = jar.getInputStream(jarEntry);
             FileOutputStream destinationFileOutputStream = new FileOutputStream(destinationFile)) {
            rewriteBytes(jarEntryInputStream, destinationFileOutputStream);
        }
    }

    private void rewriteBytes(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) >= 0) {
            fileOutputStream.write(buf, 0, len);
        }
    }
}
