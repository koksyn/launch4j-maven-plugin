package com.akathist.maven.plugins.launch4j;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactExtractorTest {
    @Mock
    private JarFileExtractor jarFileExtractor;
    @Mock
    private FileSystemUtil fileSystemUtil;
    @Mock
    private Log log;
    @Mock
    private Artifact artifact;
    @Mock
    private File packedArtifactFile;

    @InjectMocks
    private ArtifactExtractor artifactExtractor;

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenArtifact_IsNull() {
        // expect throws
        artifactExtractor.unpackAndGetUnpackedDir(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenArtifactFile_IsNull() {
        // expect throws
        artifactExtractor.unpackAndGetUnpackedDir(artifact);
    }

    @Test
    public void shouldNotUnpackDir_WhenMarkerFileExistsAndIsYounger() {
        // given
        long lastModifiedPackedArtifactFile = 1337L;
        doReturn(packedArtifactFile).when(artifact).getFile();
        doReturn(lastModifiedPackedArtifactFile).when(packedArtifactFile).lastModified();

        File topLevelDirectory = new File("temp");
        doReturn(topLevelDirectory).when(packedArtifactFile).getParentFile();

        String packedArtifactFileNameWithoutExtension = "any";
        doReturn(packedArtifactFileNameWithoutExtension)
                .when(fileSystemUtil)
                .retrieveFileNameWithoutArchiveExtension(packedArtifactFile);

        File marker = new File(topLevelDirectory, packedArtifactFileNameWithoutExtension + ".unpacked");
        doReturn(true).when(fileSystemUtil).fileExistsAndIsYoungerThan(marker, lastModifiedPackedArtifactFile);

        // when
        File unpackedDir = artifactExtractor.unpackAndGetUnpackedDir(artifact);

        // then
        verify(jarFileExtractor, never()).tryUnpackIntoDir(packedArtifactFile, topLevelDirectory);
        File expectedUnpackedDir = new File(topLevelDirectory, packedArtifactFileNameWithoutExtension);
        assertEquals(expectedUnpackedDir, unpackedDir);

        // permissions for non-Windows binaries was set
        verify(fileSystemUtil, times(2)).setNonWindowsFilePermissionsQuietly(anyString(), eq("755"));
    }

    @Test
    public void shouldUnpackDir_WhenMarkerFile_DoesNotExist_OrItIsOlder() {
        // given
        long lastModifiedPackedArtifactFile = 0L;
        doReturn(packedArtifactFile).when(artifact).getFile();
        doReturn(lastModifiedPackedArtifactFile).when(packedArtifactFile).lastModified();

        File topLevelDirectory = new File("temp");
        doReturn(topLevelDirectory).when(packedArtifactFile).getParentFile();

        String packedArtifactFileNameWithoutExtension = "any";
        doReturn(packedArtifactFileNameWithoutExtension)
                .when(fileSystemUtil)
                .retrieveFileNameWithoutArchiveExtension(packedArtifactFile);

        File marker = new File(topLevelDirectory, packedArtifactFileNameWithoutExtension + ".unpacked");
        doReturn(false).when(fileSystemUtil).fileExistsAndIsYoungerThan(marker, lastModifiedPackedArtifactFile);

        // when
        File unpackedDir = artifactExtractor.unpackAndGetUnpackedDir(artifact);

        // then
        verify(fileSystemUtil).deleteFileQuietly(marker);
        verify(jarFileExtractor).tryUnpackIntoDir(packedArtifactFile, topLevelDirectory);
        verify(fileSystemUtil).createFileQuietly(marker);

        File expectedUnpackedDir = new File(topLevelDirectory, packedArtifactFileNameWithoutExtension);
        assertEquals(expectedUnpackedDir, unpackedDir);

        // permissions for non-Windows binaries was set
        ArgumentCaptor<String> nonWindowsBinariesCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileSystemUtil, times(2))
                .setNonWindowsFilePermissionsQuietly(nonWindowsBinariesCaptor.capture(), eq("755"));
        List<String> nonWindowsBinariesPaths = nonWindowsBinariesCaptor.getAllValues();

        assertNotNull(nonWindowsBinariesPaths);
        assertEquals(2, nonWindowsBinariesPaths.size());
        assertTrue(nonWindowsBinariesPaths.stream().anyMatch(path -> path.endsWith("/bin/ld")));
        assertTrue(nonWindowsBinariesPaths.stream().anyMatch(path -> path.endsWith("/bin/windres")));
    }
}
