package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class Launch4JArtifactResolverTest {
    @Mock
    private Log log;
    @Mock
    private ArtifactResolver artifactResolver;
    @Mock
    private RepositorySystem repositorySystemFactory;
    @Mock
    private ArtifactVersionDetector artifactVersionDetector;
    @Mock
    private PlatformDetector platformDetector;
    @Mock
    private ProjectBuildingRequest configuration;
    @Mock
    private Artifact artifact;

    @InjectMocks
    private Launch4jArtifactResolver l4jArtifactResolver;

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNullConfiguration() {
        // expect throws
        l4jArtifactResolver.resolveArtifact(null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_WhenCannotDetectLaunch4jVersion() {
        // given
        doThrow(new RuntimeException("error"))
                .when(artifactVersionDetector)
                .detect(Launch4jArtifactResolver.LAUNCH4J_ARTIFACT_ID, Launch4jArtifactResolver.LAUNCH4J_GROUP_ID);

        // expect throws
        l4jArtifactResolver.resolveArtifact(configuration);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_WhenCannotResolveArtifact_BecauseOfIllegalArgument() throws Exception {
        // given
        doReturn("any_version")
                .when(artifactVersionDetector)
                .detect(Launch4jArtifactResolver.LAUNCH4J_ARTIFACT_ID, Launch4jArtifactResolver.LAUNCH4J_GROUP_ID);

        doReturn(artifact).when(repositorySystemFactory).createArtifactWithClassifier(
                eq(Launch4jArtifactResolver.LAUNCH4J_GROUP_ID),
                eq(Launch4jArtifactResolver.LAUNCH4J_ARTIFACT_ID),
                anyString(),
                anyString(),
                anyString()
        );

        doThrow(new IllegalArgumentException("error"))
                .when(artifactResolver)
                .resolveArtifact(configuration, artifact);

        // expect throws
        l4jArtifactResolver.resolveArtifact(configuration);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_WhenCannotResolveArtifact_ByArtifactResolver() throws Exception {
        // given
        doReturn(artifact).when(repositorySystemFactory).createArtifactWithClassifier(
                eq(Launch4jArtifactResolver.LAUNCH4J_GROUP_ID),
                eq(Launch4jArtifactResolver.LAUNCH4J_ARTIFACT_ID),
                anyString(),
                anyString(),
                anyString()
        );


        doReturn("some_version")
                .when(artifactVersionDetector)
                .detect(Launch4jArtifactResolver.LAUNCH4J_ARTIFACT_ID, Launch4jArtifactResolver.LAUNCH4J_GROUP_ID);

        doThrow(new ArtifactResolverException("error", new RuntimeException()))
                .when(artifactResolver)
                .resolveArtifact(configuration, artifact);

        // expect throws
        l4jArtifactResolver.resolveArtifact(configuration);
    }

    @Test
    public void shouldResolveArtifact() throws Exception {
        // given
        String launch4jVersion = "1.0.0";
        doReturn(launch4jVersion)
                .when(artifactVersionDetector)
                .detect(Launch4jArtifactResolver.LAUNCH4J_ARTIFACT_ID, Launch4jArtifactResolver.LAUNCH4J_GROUP_ID);

        String osPlatform = "linux64";
        doReturn(osPlatform).when(platformDetector).detectOSFromSystemProperties();

        doReturn(artifact).when(repositorySystemFactory).createArtifactWithClassifier(
                Launch4jArtifactResolver.LAUNCH4J_GROUP_ID,
                Launch4jArtifactResolver.LAUNCH4J_ARTIFACT_ID,
                launch4jVersion,
                "jar",
                "workdir-" + osPlatform
        );

        doReturn(mock(ArtifactResult.class))
                .when(artifactResolver)
                .resolveArtifact(any(ProjectBuildingRequest.class), any(Artifact.class));

        // when
        Artifact launch4jArtifactDefinition = l4jArtifactResolver.resolveArtifact(configuration);

        // then
        assertNotNull(launch4jArtifactDefinition);
        assertEquals(artifact, launch4jArtifactDefinition);

        verify(artifactResolver).resolveArtifact(configuration, artifact);
    }
}
