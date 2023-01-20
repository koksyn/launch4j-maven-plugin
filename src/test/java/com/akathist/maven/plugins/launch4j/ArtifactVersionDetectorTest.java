package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ArtifactVersionDetectorTest {
    private static final String ARTIFACT_ID = "any";
    private static final String GROUP_ID = "any";

    private Log log;
    private Artifact pluginArtifact;
    private ArtifactVersionDetector detector;

    @Before
    public void initialize() {
        pluginArtifact = mock(Artifact.class);
        List<Artifact> pluginArtifacts = singletonList(pluginArtifact);
        log = mock(Log.class);

        detector = new ArtifactVersionDetector(pluginArtifacts, log);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNull_ArtifactId() {
        // expect throws
        detector.detect(null, GROUP_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNull_GroupId() {
        // expect throws
        detector.detect(ARTIFACT_ID, null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotFindVersion_WhenGroupIdWasNotMatched() {
        // given
        doReturn("differentGroupId").when(pluginArtifact).getGroupId();

        // expect throws
        detector.detect(ARTIFACT_ID, GROUP_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotFindVersion_WhenArtifactIdWasNotMatched() {
        // given
        doReturn(GROUP_ID).when(pluginArtifact).getGroupId();
        doReturn("differentArtifactId").when(pluginArtifact).getArtifactId();

        // expect throws
        detector.detect(ARTIFACT_ID, GROUP_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotFindVersion_WhenArtifactClassifierWasNotMatched() {
        // given
        doReturn(GROUP_ID).when(pluginArtifact).getGroupId();
        doReturn(ARTIFACT_ID).when(pluginArtifact).getArtifactId();
        doReturn("not Core!").when(pluginArtifact).getClassifier();

        // expect throws
        detector.detect(ARTIFACT_ID, GROUP_ID);
    }

    @Test
    public void shouldFindVersion() {
        // given
        doReturn(GROUP_ID).when(pluginArtifact).getGroupId();
        doReturn(ARTIFACT_ID).when(pluginArtifact).getArtifactId();
        doReturn("core").when(pluginArtifact).getClassifier();

        String expectedVersion = "1.0.0";
        doReturn(expectedVersion).when(pluginArtifact).getVersion();

        // when
        String detectedVersion = detector.detect(ARTIFACT_ID, GROUP_ID);

        // then
        assertEquals(expectedVersion, detectedVersion);
    }
}
