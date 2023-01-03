package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import static com.akathist.maven.plugins.launch4j.utils.ToStringVerifier.containsParam;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ClassPathTest {
    // Params
    private final String MAIN_CLASS = "Main";
    private final boolean ADD_DEPENDENCIES = true;
    private final String JAR_LOCATION = "target/output.jar";
    private final String PRE_CP = "/var/log;/opt";
    private final String POST_CP = "/etc/hosts;/etc/dummy";

    // Subject
    private ClassPath classPath;

    @Before
    public void buildFromTestParams() {
        classPath = new ClassPath(MAIN_CLASS, ADD_DEPENDENCIES, JAR_LOCATION, PRE_CP, POST_CP);
    }

    @Test
    public void shouldConvert_MainClass_IntoL4jFormat() {
        // given
        Set<Artifact> dependencies = null;

        // when
        net.sf.launch4j.config.ClassPath l4jClassPath = classPath.toL4j(dependencies);

        // then
        assertNotNull(l4jClassPath);
        assertEquals(MAIN_CLASS, l4jClassPath.getMainClass());
    }

    @Test
    public void shouldConvert_Paths_IntoL4jFormat_With_JarLocation_And_Dependencies_Provided() {
        // given
        File dependencyFile = mock(File.class);
        String dependencyFilename = "abc/def.xml";
        doReturn(dependencyFilename).when(dependencyFile).getName();

        Artifact dependency = mock(Artifact.class);
        doReturn(dependencyFile).when(dependency).getFile();

        Set<Artifact> dependencies = singleton(dependency);

        // when
        net.sf.launch4j.config.ClassPath l4jClassPath = classPath.toL4j(dependencies);

        // then
        assertNotNull(l4jClassPath);

        String expectedClassPathString = String.join(
                ";",
                Arrays.asList(PRE_CP, JAR_LOCATION + '/' + dependencyFilename, POST_CP)
        );
        assertEquals(expectedClassPathString, l4jClassPath.getPathsString());
    }

    @Test
    public void shouldConvert_Paths_IntoL4jFormat_Without_JarLocation_And_With_Dependencies_Provided() {
        // given
        File dependencyFile = mock(File.class);
        String dependencyFilename = "qwerty/ip.xml";
        doReturn(dependencyFilename).when(dependencyFile).getName();

        Artifact dependency = mock(Artifact.class);
        doReturn(dependencyFile).when(dependency).getFile();

        Set<Artifact> dependencies = singleton(dependency);

        // when
        classPath.jarLocation = null;
        net.sf.launch4j.config.ClassPath l4jClassPath = classPath.toL4j(dependencies);

        // then
        assertNotNull(l4jClassPath);

        String expectedClassPathString = String.join(
                ";",
                Arrays.asList(PRE_CP, dependencyFilename, POST_CP)
        );
        assertEquals(expectedClassPathString, l4jClassPath.getPathsString());
    }

    @Test
    public void shouldConvert_Paths_IntoL4jFormat_Without_Dependencies_Provided() {
        // given
        Set<Artifact> dependencies = null;

        // when
        classPath.preCp = null;
        classPath.postCp = null;
        net.sf.launch4j.config.ClassPath l4jClassPathForNoPaths = classPath.toL4j(dependencies);

        classPath.preCp = PRE_CP;
        classPath.postCp = null;
        net.sf.launch4j.config.ClassPath l4jClassPathForOnlyPreCp = classPath.toL4j(dependencies);

        classPath.preCp = null;
        classPath.postCp = POST_CP;
        net.sf.launch4j.config.ClassPath l4jClassPathForOnlyPostCp = classPath.toL4j(dependencies);

        classPath.preCp = PRE_CP;
        classPath.postCp = POST_CP;
        net.sf.launch4j.config.ClassPath l4jClassPathForAllPaths = classPath.toL4j(dependencies);

        // then
        assertNotNull(l4jClassPathForNoPaths);
        assertNotNull(l4jClassPathForOnlyPreCp);
        assertNotNull(l4jClassPathForOnlyPostCp);
        assertNotNull(l4jClassPathForAllPaths);


        assertTrue(l4jClassPathForNoPaths.getPathsString().isEmpty());

        String onlyPreCpFilled = String.join(";", singletonList(PRE_CP));
        assertEquals(onlyPreCpFilled, l4jClassPathForOnlyPreCp.getPathsString());

        String onlyPostCpFilled = String.join(";", singletonList(POST_CP));
        assertEquals(onlyPostCpFilled, l4jClassPathForOnlyPostCp.getPathsString());

        String allPathsFilled = String.join(";", Arrays.asList(PRE_CP, POST_CP));
        assertEquals(allPathsFilled, l4jClassPathForAllPaths.getPathsString());
    }

    @Test
    public void shouldGenerateString_WithTestParams() {
        // when
        String result = classPath.toString();

        // then
        assertNotNull(result);
        assertTrue(containsParam(result, "mainClass", MAIN_CLASS));
        assertTrue(containsParam(result, "addDependencies", String.valueOf(ADD_DEPENDENCIES)));
        assertTrue(containsParam(result, "jarLocation", JAR_LOCATION));
        assertTrue(containsParam(result, "preCp", PRE_CP));
        assertTrue(containsParam(result, "postCp", POST_CP));
    }
}
