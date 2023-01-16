package com.akathist.maven.plugins.launch4j;

import net.sf.launch4j.BuilderException;
import org.apache.maven.plugin.logging.Log;
import net.sf.launch4j.Builder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExecutableBuilderTest {
    @Mock
    private Builder builder;
    @Mock
    private File baseDirectory;
    @Mock
    private Log log;
    @Mock
    private ExecutableBuilderFactory executableBuilderFactory;

    @InjectMocks
    private ExecutableBuilder executableBuilder;

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_WhenBaseDirectoryIs_Null() {
        executableBuilder.build(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_WhenBaseDirectoryDoes_NotExist() {
        // given
        doReturn(false).when(baseDirectory).exists();

        // expect throws
        executableBuilder.build(baseDirectory);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_WhenBuilderThrowsException() throws BuilderException {
        // given
        doReturn(true).when(baseDirectory).exists();

        doReturn(builder).when(executableBuilderFactory).build(any(MavenLog.class), eq(baseDirectory));
        doThrow(new BuilderException()).when(builder).build();

        // expect throws
        executableBuilder.build(baseDirectory);
    }

    @Test
    public void shouldBuildExecutable_WhenBaseDirectoryDoes_Exist() throws BuilderException {
        // given
        doReturn(true).when(baseDirectory).exists();

        ArgumentCaptor<MavenLog> mavenLogCaptor = ArgumentCaptor.forClass(MavenLog.class);
        doReturn(builder).when(executableBuilderFactory).build(mavenLogCaptor.capture(), eq(baseDirectory));

        // when
        executableBuilder.build(baseDirectory);

        // then
        verify(builder).build();

        MavenLog mavenLog = mavenLogCaptor.getValue();
        assertNotNull(mavenLog);
        assertEquals(log, mavenLog._log);
    }
}
