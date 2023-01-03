package com.akathist.maven.plugins.launch4j;

import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static com.akathist.maven.plugins.launch4j.utils.ToStringVerifier.containsParam;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JreTest {
    // Params
    private final String PATH = "/var/jre/jre_1.8_200";
    private final String BUNDLED_JRE64_BIT = "false";
    private final String BUNDLED_JRE_AS_FALLBACK = "false";
    private final boolean REQUIRES64_BIT = false;
    private final String MIN_VERSION = "1.0.0.0";
    private final String MAX_VERSION = "1.9.9.9";
    private final String JDK_PREFERENCE = "preferJre";
    private final boolean REQUIRES_JDK = false;
    private final Integer INITIAL_HEAP_SIZE = 512;
    private final Integer INITIAL_HEAP_PERCENT = 20;
    private final Integer MAX_HEAP_SIZE = 4096;
    private final Integer MAX_HEAP_PERCENT = 75;
    private final String OPTION = "&lt;opt&gt;-Dlaunch4j.exedir=\\\"%EXEDIR%\\\"&lt;/opt&gt;";
    private final List<String> OPTIONS = singletonList(OPTION);
    private final String RUNTIME_BITS = "64/32";

    // Mocks
    @Mock
    Log log;

    // Subject
    private Jre jre;

    @Before
    public void buildFromTestParams() {
        jre = new Jre(PATH, BUNDLED_JRE64_BIT, BUNDLED_JRE_AS_FALLBACK,
                REQUIRES64_BIT, MIN_VERSION, MAX_VERSION,
                JDK_PREFERENCE, REQUIRES_JDK, INITIAL_HEAP_SIZE,
                INITIAL_HEAP_PERCENT, MAX_HEAP_SIZE, MAX_HEAP_PERCENT,
                OPTIONS, RUNTIME_BITS);
    }

    @Test
    public void shouldConvertIntoL4jFormatProperly() {
        // when
        net.sf.launch4j.config.Jre l4jJre = jre.toL4j();

        // then
        assertNotNull(l4jJre);
        assertEquals(PATH, l4jJre.getPath());
        assertEquals(REQUIRES64_BIT, l4jJre.getRequires64Bit());
        assertEquals(MIN_VERSION, l4jJre.getMinVersion());
        assertEquals(MAX_VERSION, l4jJre.getMaxVersion());
        assertEquals(REQUIRES_JDK, l4jJre.getRequiresJdk());
        assertEquals(INITIAL_HEAP_SIZE, l4jJre.getInitialHeapSize());
        assertEquals(INITIAL_HEAP_PERCENT, l4jJre.getInitialHeapPercent());
        assertEquals(MAX_HEAP_SIZE, l4jJre.getMaxHeapSize());
        assertEquals(MAX_HEAP_PERCENT, l4jJre.getMaxHeapPercent());
        assertEquals(OPTIONS, l4jJre.getOptions());
    }

    @Test
    public void shouldGenerateString_WithTestParams() {
        // when
        String result = jre.toString();

        // then
        assertNotNull(result);
        assertTrue(containsParam(result, "path", PATH));
        assertTrue(containsParam(result, "requires64Bit", String.valueOf(REQUIRES64_BIT)));
        assertTrue(containsParam(result, "minVersion", MIN_VERSION));
        assertTrue(containsParam(result, "maxVersion", MAX_VERSION));
        assertTrue(containsParam(result, "requiresJdk", String.valueOf(REQUIRES_JDK)));
        assertTrue(containsParam(result, "initialHeapSize", String.valueOf(INITIAL_HEAP_SIZE)));
        assertTrue(containsParam(result, "initialHeapPercent", String.valueOf(INITIAL_HEAP_PERCENT)));
        assertTrue(containsParam(result, "maxHeapSize", String.valueOf(MAX_HEAP_SIZE)));
        assertTrue(containsParam(result, "maxHeapPercent", String.valueOf(MAX_HEAP_PERCENT)));
        assertTrue(containsParam(result, "opts", OPTIONS.toString()));
    }

    @Test
    public void shouldWarnAboutDeprecatedProperties_WhenTheyWere_Filled() {
        // given
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // when
        jre.deprecationWarning(log);

        // then
        verify(log, times(4)).warn(messageCaptor.capture());

        String capturedMessages = String.join("", messageCaptor.getAllValues());
        assertTrue(capturedMessages.contains("bundledJreAsFallback"));
        assertTrue(capturedMessages.contains("bundledJre64Bit"));
        assertTrue(capturedMessages.contains("runtimeBits"));
        assertTrue(capturedMessages.contains("jdkPreference"));
    }

    @Test
    public void should_Not_WarnAboutDeprecatedProperties_WhenTheyWere_Not_Filled() {
        // given
        jre.bundledJreAsFallback = null;
        jre.bundledJre64Bit = null;
        jre.runtimeBits = null;
        jre.jdkPreference = null;

        // when
        jre.deprecationWarning(log);

        // then
        verify(log, never()).warn(anyString());
    }
}
