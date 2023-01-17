package com.akathist.maven.plugins.launch4j;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemUtilTest {
    @Mock
    private File subject;
    @Mock
    private File parentFolder;

    @Mock
    private Log log;

    @InjectMocks
    private FileSystemUtil fileSystemUtil;

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
}
