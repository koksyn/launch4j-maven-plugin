package com.akathist.maven.plugins.launch4j;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnitParamsRunner.class)
public class FileSystemUtilTest {
    @Mock
    private File subject;
    @Mock
    private File parentFolder;

    @Mock
    private Log log;

    @InjectMocks
    private FileSystemUtil fileSystemUtil;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void should_Not_CreateParentFolderWhen_SubjectIsNull() {
        // when
        fileSystemUtil.createParentFolderQuietly(null);

        // then
        verify(parentFolder, never()).mkdirs();
    }

    @Test
    public void should_Not_CreateParentFolderWhen_ItDoesExist() {
        // given
        doReturn(parentFolder).when(subject).getParentFile();
        doReturn(true).when(parentFolder).exists();

        // when
        fileSystemUtil.createParentFolderQuietly(subject);

        // then
        verify(parentFolder, never()).mkdirs();
    }

    @Test
    public void shouldLogWarningWhen_CannotCreateParentFolder() {
        // given
        ArgumentCaptor<String> logMessageCaptor = ArgumentCaptor.forClass(String.class);

        doReturn(parentFolder).when(subject).getParentFile();
        doReturn(false).when(parentFolder).exists();
        doReturn(false).when(parentFolder).mkdirs();

        // when
        fileSystemUtil.createParentFolderQuietly(subject);

        // then
        verify(parentFolder).mkdirs();
        verify(log).warn(logMessageCaptor.capture());

        String warnLogMessage = logMessageCaptor.getValue();
        assertNotNull(warnLogMessage);
        assertTrue(warnLogMessage.contains("Cannot create parent"));
    }

    @Test
    public void shouldCreateParentFolderWhen_ItDoesNotExist() {
        // given
        doReturn(parentFolder).when(subject).getParentFile();
        doReturn(false).when(parentFolder).exists();
        doReturn(true).when(parentFolder).mkdirs();

        // when
        fileSystemUtil.createParentFolderQuietly(subject);

        // then
        verify(parentFolder).mkdirs();
        verify(log, never()).warn(anyString());
    }

    @Test
    public void should_Not_CreateFileWhen_ItIsNull() {
        // when
        fileSystemUtil.createFileQuietly(null);

        // then
        verify(log, never()).warn(anyString());
    }

    @Test
    public void shouldLogWarn_WhenIOExceptionOccurred() throws IOException {
        // given
        IOException ioException = new IOException();
        doThrow(ioException).when(subject).createNewFile();

        // when
        fileSystemUtil.createFileQuietly(subject);

        // then
        verify(log).warn(anyString(), eq(ioException));
    }

    @Test
    public void shouldCreateFileWhen_ItDoesNotExist() throws IOException {
        // when
        fileSystemUtil.createFileQuietly(subject);

        // then
        verify(subject).createNewFile();
        verify(subject).setLastModified(anyLong());
        verify(log, never()).warn(anyString());
    }

    @Test
    public void should_Not_DeleteFileWhen_ItIsNull() {
        // when
        fileSystemUtil.deleteFileQuietly(null);

        // then
        verify(log, never()).warn(anyString());
    }

    @Test
    public void shouldDeleteFile() throws IOException {
        // given
        File tempFile = new File("temp");
        FileUtils.touch(tempFile);
        doReturn(tempFile.toPath()).when(subject).toPath();

        // when
        fileSystemUtil.deleteFileQuietly(subject);

        // then
        assertFalse(tempFile.exists());
        verify(log, never()).warn(anyString());
    }

    @Test
    public void shouldReturnFalse_WhenFileDoesNotExist() {
        // when
        boolean result = fileSystemUtil.fileExistsAndIsYoungerThan(subject, 0L);

        // then
        assertFalse(result);
    }

    @Test
    public void shouldReturnFalse_WhenFileDoesExist_AndIsNotYoungerThanTimestamp() {
        // given
        long lastModified = 1L;
        long timestamp = 1337L;

        doReturn(true).when(subject).exists();
        doReturn(lastModified).when(subject).lastModified();

        // when
        boolean result = fileSystemUtil.fileExistsAndIsYoungerThan(subject, timestamp);

        // then
        assertFalse(result);
    }

    @Test
    public void shouldReturnTrue_WhenFileDoesExist_AndIsYoungerThanTimestamp() {
        // given
        long lastModified = 123L;
        long timestamp = 111L;

        doReturn(true).when(subject).exists();
        doReturn(lastModified).when(subject).lastModified();

        // when
        boolean result = fileSystemUtil.fileExistsAndIsYoungerThan(subject, timestamp);

        // then
        assertTrue(result);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowException_WhenFileIsNull() {
        // expect throws
        fileSystemUtil.fileLocatedOutsideDir(null, parentFolder);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowException_WhenDirIsNull() {
        // expect throws
        fileSystemUtil.fileLocatedOutsideDir(subject, null);
    }

    @Test
    @Parameters({
        "/tmp/file,/tmp",
        "/file,/",
        "abcd/efgh,abcd",
    })
    public void shouldReturnFalse_WhenFileLocatedInsideDir(String filePath, String dirPath) {
        // given
        File file = new File(filePath);
        File dir = new File(dirPath);

        // when
        boolean result = fileSystemUtil.fileLocatedOutsideDir(file, dir);

        // then
        assertFalse(result);
    }

    @Test
    @Parameters({
            "/tmp/file,/opt",
            "/file,/dir",
            "hello.world,root",
    })
    public void shouldReturnTrue_WhenFileLocatedOutsideDir(String filePath, String dirPath) {
        // given
        File file = new File(filePath);
        File dir = new File(dirPath);

        // when
        boolean result = fileSystemUtil.fileLocatedOutsideDir(file, dir);

        // then
        assertTrue(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_WhenTryingToRetrieveFileName_FromNullFile() {
        // expect throws
        fileSystemUtil.retrieveFileNameWithoutArchiveExtension(null);
    }

    @Test
    @Parameters({
        "app.exe",
        "something/to/say.txt",
        "nothing.to.say"
    })
    public void shouldRetrieve_FullFileName_WhenFileIsNotNull_AndDoesNotContainArchiveExtension(String fullFileName) {
        // given
        doReturn(fullFileName).when(subject).getName();

        // when
        String fileName = fileSystemUtil.retrieveFileNameWithoutArchiveExtension(subject);

        // then
        assertEquals(fullFileName, fileName);
    }

    @Test
    @Parameters({
            "sth.zip, sth",
            "super/java/app.jar, super/java/app",
            "artifact.jar, artifact"
    })
    public void shouldRetrieve_LimitedFileName_WhenFileIsNotNull_AndContainsArchiveExtension(
        String fullFileName,
        String limitedFileName
    ) {
        // given
        doReturn(fullFileName).when(subject).getName();

        // when
        String fileName = fileSystemUtil.retrieveFileNameWithoutArchiveExtension(subject);

        // then
        assertEquals(limitedFileName, fileName);
    }
}
