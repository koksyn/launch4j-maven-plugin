package com.akathist.maven.plugins.launch4j;

import net.sf.launch4j.Builder;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ExecutableBuilderFactoryTest {
    @Test
    public void shouldBuildLaunch4jBuilder() {
        // given
        ExecutableBuilderFactory factory = new ExecutableBuilderFactory();
        MavenLog mavenLog = mock(MavenLog.class);
        File buildDirectory = mock(File.class);

        // when
        Builder builder = factory.build(mavenLog, buildDirectory);

        // then
        assertNotNull(builder);
    }
}
