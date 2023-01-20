package com.akathist.maven.plugins.launch4j;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class JarFileExtractorTest {
    @Mock
    private Log log;
    private FileSystemUtil fileSystemUtil;
    private JarFileExtractor jarFileExtractor;
    private File sourceDir = new File("temp_source");
    private File destinationDir = new File("temp_destination");
    private File jar = new File(destinationDir + "/app.jar");
    private File jarOutsideDestinationDir = new File("outsider.jar");

    @Before
    public void setUp() throws IOException {
        fileSystemUtil = new FileSystemUtil(log);
        jarFileExtractor = new JarFileExtractor(fileSystemUtil);

        FileUtils.forceMkdir(sourceDir);
        FileUtils.forceMkdir(destinationDir);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteQuietly(jar);
        FileUtils.deleteQuietly(jarOutsideDestinationDir);
        FileUtils.deleteDirectory(destinationDir);
        FileUtils.deleteDirectory(sourceDir);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_WhenFileInsideJarIsNull() {
        // expect throws
        jarFileExtractor.tryUnpackIntoDir(null, new File(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_WhenDestinationDirIsNull() {
        // expect throws
        jarFileExtractor.tryUnpackIntoDir(new File(""), null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_WhenJarDoesNotExist() {
        // expect throws
        jarFileExtractor.tryUnpackIntoDir(new File("nonExistingJar"), new File("nonExistingDir"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_WhenJarLocatedOutsideDestinationDir() throws IOException {
        // given
        FileUtils.touch(jarOutsideDestinationDir);

        // expect throws
        jarFileExtractor.tryUnpackIntoDir(jarOutsideDestinationDir, destinationDir);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_WhenJarFileIsEmpty() throws IOException {
        // given
        FileUtils.touch(jar);

        // expect throws
        jarFileExtractor.tryUnpackIntoDir(jar, destinationDir);
    }

    @Test
    public void shouldUnpackJar() throws IOException {
        // given
        String fileName = "source.txt";
        String fileContent = "hello";
        long lastModified = 1337L;
        // Unfortunately a JarArchiver has an issue, which modifies a little the lastModified date inside Jar
        // but after unpacking it remains the same, which is desirable behaviour of this test
        long lastModifiedPlexusArchiverModified = 312779280000L;

        createSourceFile(fileName, fileContent, lastModified);
        packSourceFileIntoJar();

        // when
        jarFileExtractor.tryUnpackIntoDir(jar, destinationDir);

        // then
        File unpackedFile = new File(destinationDir + "/" + fileName);
        assertTrue(unpackedFile.exists());
        assertEquals(fileContent, FileUtils.readFileToString(unpackedFile));

        FileTime lastModifiedTime = Files.getLastModifiedTime(unpackedFile.toPath());
        assertEquals(lastModifiedPlexusArchiverModified, lastModifiedTime.toMillis());
    }

    private void createSourceFile(String fileName, String fileContent, long lastModified) throws IOException {
        File sourceFile = new File(sourceDir + "/" + fileName);
        FileUtils.touch(sourceFile);
        FileUtils.write(sourceFile, fileContent);
        sourceFile.setLastModified(lastModified);
    }

    private void packSourceFileIntoJar() throws IOException {
        Archiver archiver = new JarArchiver();
        archiver.addDirectory(sourceDir);
        archiver.setDestFile(jar);
        archiver.createArchive();
    }
}
