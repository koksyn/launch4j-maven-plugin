package com.akathist.maven.plugins.launch4j;

import net.sf.launch4j.config.LanguageID;
import org.apache.maven.model.Organization;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.akathist.maven.plugins.launch4j.utils.ToStringVerifier.containsParam;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionInfoTest {
    // VersionInfo test params
    private final String FILE_VERSION = "1.0.0.0";
    private final String TXT_FILE_VERSION = "1.0.0.0";
    private final String FILE_DESCRIPTION = "Launch4j Test Application";
    private final String COPYRIGHT = "Copyright Orphan OSS";
    private final String PRODUCT_VERSION = "1.0.0.0";
    private final String TXT_PRODUCT_VERSION = "1.0.0.0";
    private final String PRODUCT_NAME = "Test App";
    private final String COMPANY_NAME = "Orphan OSS Company";
    private final String INTERNAL_NAME = "app";
    private final String ORIGINAL_FILENAME = "app.exe";
    private final String LANGUAGE = LanguageID.ENGLISH_US.name();
    private final String TRADEMARKS = "Test ™";

    // Mocks
    @Mock
    Organization organization;
    @Mock
    MavenProject project;
    @Mock
    File outfile;
    @Mock
    Log log;

    // Subject
    private VersionInfo versionInfo;

    @Before
    public void buildVersionInfoFromTestParams() {
        versionInfo = new VersionInfo(FILE_VERSION, TXT_FILE_VERSION, FILE_DESCRIPTION,
                COPYRIGHT, PRODUCT_VERSION, TXT_PRODUCT_VERSION,
                PRODUCT_NAME, COMPANY_NAME, INTERNAL_NAME,
                ORIGINAL_FILENAME, LANGUAGE, TRADEMARKS, log);
    }

    @Test
    public void shouldConvertIntoL4jFormatProperly() {
        // when
        net.sf.launch4j.config.VersionInfo l4jVersionInfo = versionInfo.toL4j();

        // then
        assertNotNull(l4jVersionInfo);
        assertEquals(versionInfo.fileVersion, l4jVersionInfo.getFileVersion());
        assertEquals(versionInfo.txtFileVersion, l4jVersionInfo.getTxtFileVersion());
        assertEquals(versionInfo.fileDescription, l4jVersionInfo.getFileDescription());
        assertEquals(versionInfo.copyright, l4jVersionInfo.getCopyright());
        assertEquals(versionInfo.productVersion, l4jVersionInfo.getProductVersion());
        assertEquals(versionInfo.txtProductVersion, l4jVersionInfo.getTxtProductVersion());
        assertEquals(versionInfo.productName, l4jVersionInfo.getProductName());
        assertEquals(versionInfo.companyName, l4jVersionInfo.getCompanyName());
        assertEquals(versionInfo.internalName, l4jVersionInfo.getInternalName());
        assertEquals(versionInfo.originalFilename, l4jVersionInfo.getOriginalFilename());
        assertEquals(versionInfo.trademarks, l4jVersionInfo.getTrademarks());
        assertEquals(versionInfo.language, l4jVersionInfo.getLanguage().name());
    }

    @Test
    public void shouldConvertIntoL4jFormat_For_All_Languages() {
        for (LanguageID languageId : LanguageID.values()) {
            // given
            versionInfo.language = languageId.name();

            // when
            net.sf.launch4j.config.VersionInfo l4jVersionInfo = versionInfo.toL4j();

            // then
            assertEquals(languageId, l4jVersionInfo.getLanguage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_WhenTryingToFillOutDefaults_WithEmptyProject() {
        // expect throws
        versionInfo.tryFillOutByDefaults(null, outfile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_WhenTryingToFillOutDefaults_WithEmptyOutfile() {
        // expect throws
        versionInfo.tryFillOutByDefaults(project, null);
    }

    @Test
    public void should_Not_FillOut_ByDefaultVersion_InL4jFormat_When_VersionInfoPropsWere_Filled() {
        // given
        String projectVersion = "4.3.2.1";
        doReturn(projectVersion).when(project).getVersion();

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertNotEquals(projectVersion, versionInfo.fileVersion);
        assertEquals(FILE_VERSION, versionInfo.fileVersion);
        assertNotEquals(projectVersion, versionInfo.productVersion);
        assertEquals(PRODUCT_VERSION, versionInfo.productVersion);
    }

    @Test
    public void shouldFillOut_ByDefaultVersion_InL4jFormat_When_VersionInfoPropsWere_Empty() {
        // given
        String projectVersion = "1.2.3.4";
        doReturn(projectVersion).when(project).getVersion();

        versionInfo.fileVersion = null;
        versionInfo.productVersion = null;

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertEquals(projectVersion, versionInfo.fileVersion);
        assertNotEquals(FILE_VERSION, versionInfo.fileVersion);
        assertEquals(projectVersion, versionInfo.productVersion);
        assertNotEquals(PRODUCT_VERSION, versionInfo.productVersion);
    }

    @Test
    public void should_Not_FillOut_Copyright_ByDefault_When_ItWas_Filled() {
        // given
        String projectInceptionYear = "2017";
        doReturn(projectInceptionYear).when(project).getInceptionYear();

        String organizationName = "Another OSS";
        doReturn(organizationName).when(organization).getName();
        doReturn(organization).when(project).getOrganization();

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertNotNull(versionInfo.copyright);
        assertEquals(COPYRIGHT, versionInfo.copyright);
        assertFalse(versionInfo.copyright.contains(projectInceptionYear));
        assertFalse(versionInfo.copyright.contains(organizationName));
    }

    @Test
    public void shouldFillOut_Copyright_ByDefault_When_ItWas_Empty() {
        // given
        String projectInceptionYear = "2019";
        doReturn(projectInceptionYear).when(project).getInceptionYear();

        String organizationName = "Some OSS";
        doReturn(organizationName).when(organization).getName();
        doReturn(organization).when(project).getOrganization();

        versionInfo.copyright = null;

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertNotNull(versionInfo.copyright);
        assertNotEquals(COPYRIGHT, versionInfo.copyright);
        assertTrue(versionInfo.copyright.contains(projectInceptionYear));
        assertTrue(versionInfo.copyright.contains(organizationName));
    }

    @Test
    public void should_Not_FillOutByDefaults_From_OrganizationName_When_VersionInfoPropsWere_Filled() {
        // given
        String organizationName = "Example OSS";
        doReturn(organizationName).when(organization).getName();
        doReturn(organization).when(project).getOrganization();

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertNotEquals(organizationName, versionInfo.companyName);
        assertEquals(COMPANY_NAME, versionInfo.companyName);
        assertNotEquals(organizationName, versionInfo.trademarks);
        assertEquals(TRADEMARKS, versionInfo.trademarks);
    }

    @Test
    public void shouldFillOutByDefaults_From_OrganizationName_When_OrganizationWas_Filled() {
        // given
        String organizationName = "Other OSS";
        doReturn(organizationName).when(organization).getName();
        doReturn(organization).when(project).getOrganization();

        versionInfo.companyName = null;
        versionInfo.trademarks = null;

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertEquals(organizationName, versionInfo.companyName);
        assertEquals(organizationName, versionInfo.trademarks);
    }

    @Test
    public void should_Not_FillOutByDefaults_SimpleValues_From_MavenProject_When_VersionInfoPropsWere_Filled() {
        // given
        String projectVersion = "1.21.1";
        doReturn(projectVersion).when(project).getVersion();

        String projectName = "launch4j-test-app";
        doReturn(projectName).when(project).getName();

        String projectArtifactId = "launch4j-test";
        doReturn(projectArtifactId).when(project).getArtifactId();

        String projectDescription = "Launch4j Test App";
        doReturn(projectDescription).when(project).getDescription();

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertNotEquals(projectVersion, versionInfo.txtFileVersion);
        assertEquals(TXT_FILE_VERSION, versionInfo.txtFileVersion);
        assertNotEquals(projectVersion, versionInfo.txtProductVersion);
        assertEquals(TXT_PRODUCT_VERSION, versionInfo.txtProductVersion);
        assertNotEquals(projectName, versionInfo.productName);
        assertEquals(PRODUCT_NAME, versionInfo.productName);
        assertNotEquals(projectArtifactId, versionInfo.internalName);
        assertEquals(INTERNAL_NAME, versionInfo.internalName);
        assertNotEquals(projectDescription, versionInfo.fileDescription);
        assertEquals(FILE_DESCRIPTION, versionInfo.fileDescription);
    }

    @Test
    public void shouldFillOutByDefaults_SimpleValues_From_MavenProject_When_VersionInfoPropsWere_Empty() {
        // given
        String projectVersion = "1.21.1";
        doReturn(projectVersion).when(project).getVersion();
        versionInfo.txtFileVersion = null;
        versionInfo.txtProductVersion = null;

        String projectName = "launch4j-test-app";
        doReturn(projectName).when(project).getName();
        versionInfo.productName = null;

        String projectArtifactId = "launch4j-test";
        doReturn(projectArtifactId).when(project).getArtifactId();
        versionInfo.internalName = null;

        String projectDescription = "Launch4j Test App";
        doReturn(projectDescription).when(project).getDescription();
        versionInfo.fileDescription = null;

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertEquals(projectVersion, versionInfo.txtFileVersion);
        assertEquals(projectVersion, versionInfo.txtProductVersion);
        assertEquals(projectName, versionInfo.productName);
        assertEquals(projectArtifactId, versionInfo.internalName);
        assertEquals(projectDescription, versionInfo.fileDescription);
    }

    @Test
    public void should_Not_FillOut_ByDefault_LastSegmentOfOutfilePath_When_OriginalFilenameWas_Filled() {
        // given
        String outfileName = "testApp.exe";
        doReturn(outfileName).when(outfile).getName();

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertNotEquals(outfileName, versionInfo.originalFilename);
        assertEquals(ORIGINAL_FILENAME, versionInfo.originalFilename);
    }

    @Test
    public void shouldFillOut_ByDefault_LastSegmentOfOutfilePath_When_OriginalFilenameWas_Empty() {
        // given
        String outfileName = "testApp.exe";
        doReturn(outfileName).when(outfile).getName();
        versionInfo.originalFilename = null;

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertEquals(outfileName, versionInfo.originalFilename);
    }

    @Test
    public void shouldLogWarningsAboutDummyValues() {
        // given
        ArgumentCaptor<String> logMessageCaptor = ArgumentCaptor.forClass(String.class);
        List<String> missingParamNames = Arrays.asList(
                "project.version",
                "project.name",
                "project.artifactId",
                "project.description",
                "project.inceptionYear",
                "project.organization.name",
                "outfile"
        );

        // when
        versionInfo.tryFillOutByDefaults(project, outfile);

        // then
        verify(log, times(missingParamNames.size())).warn(logMessageCaptor.capture());
        List<String> logMessages = logMessageCaptor.getAllValues();


        missingParamNames.forEach(missingParamName -> {
            assertTrue(logMessages.stream().anyMatch(message -> message.contains(missingParamName)));
        });
    }

    @Test
    public void shouldFillOut_ByDummyValues_When_OriginalValues_Empty_And_ProjectParams_Empty() {
        // given
        final String buildYear = String.valueOf(LocalDate.now().getYear());

        VersionInfo emptyValuesVersionInfo = new VersionInfo();
        emptyValuesVersionInfo.setLog(log);

        // when
        emptyValuesVersionInfo.tryFillOutByDefaults(project, outfile);

        // then
        assertEquals("1.0.0.0", emptyValuesVersionInfo.fileVersion);
        assertEquals("1.0.0", emptyValuesVersionInfo.txtFileVersion);
        assertEquals("A Java project.", emptyValuesVersionInfo.fileDescription);
        assertEquals("Copyright © 2020-" + buildYear + " Default organization. All rights reserved.", emptyValuesVersionInfo.copyright);
        assertEquals("1.0.0.0", emptyValuesVersionInfo.productVersion);
        assertEquals("1.0.0", emptyValuesVersionInfo.txtProductVersion);
        assertEquals("Java Project", emptyValuesVersionInfo.productName);
        assertEquals("Default organization", emptyValuesVersionInfo.companyName);
        assertEquals("java-project", emptyValuesVersionInfo.internalName);
        assertEquals("Default organization", emptyValuesVersionInfo.trademarks);
        assertEquals("app.exe", emptyValuesVersionInfo.originalFilename);
    }

    @Test
    public void shouldGenerateString_WithTestParams() {
        // when
        String result = versionInfo.toString();

        // then
        assertNotNull(result);
        assertTrue(containsParam(result, "fileVersion", FILE_VERSION));
        assertTrue(containsParam(result, "txtFileVersion", TXT_FILE_VERSION));
        assertTrue(containsParam(result, "fileDescription", FILE_DESCRIPTION));
        assertTrue(containsParam(result, "copyright", COPYRIGHT));
        assertTrue(containsParam(result, "productVersion", PRODUCT_VERSION));
        assertTrue(containsParam(result, "txtProductVersion", TXT_PRODUCT_VERSION));
        assertTrue(containsParam(result, "productName", PRODUCT_NAME));
        assertTrue(containsParam(result, "companyName", COMPANY_NAME));
        assertTrue(containsParam(result, "internalName", INTERNAL_NAME));
        assertTrue(containsParam(result, "originalFilename", ORIGINAL_FILENAME));
        assertTrue(containsParam(result, "language", LANGUAGE));
        assertTrue(containsParam(result, "trademarks", TRADEMARKS));
    }
}
