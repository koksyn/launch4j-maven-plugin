package com.akathist.maven.plugins.launch4j;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static com.akathist.maven.plugins.launch4j.utils.ToStringVerifier.containsParam;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class SplashTest {
    // Splash test params
    private final boolean WAIT_FOR_WINDOW = true;
    private final int TIMEOUT = 60;
    private final boolean TIMEOUT_ERR = true;

    // Mocks
    @Mock
    private File file;

    // Subject
    private Splash splash;

    @Before
    public void buildFromTestParams() {
        splash = new Splash(file, WAIT_FOR_WINDOW, TIMEOUT, TIMEOUT_ERR);
    }

    @Test
    public void shouldConvertIntoL4jFormatProperly() {
        // when
        net.sf.launch4j.config.Splash l4jSplash = splash.toL4j();

        // then
        assertNotNull(l4jSplash);
        assertEquals(file, l4jSplash.getFile());
        assertEquals(WAIT_FOR_WINDOW, l4jSplash.getWaitForWindow());
        assertEquals(TIMEOUT, l4jSplash.getTimeout());
        assertEquals(TIMEOUT_ERR, l4jSplash.isTimeoutErr());
    }

    @Test
    public void shouldGenerateString_WithTestParams() {
        // given
        String filePath = "example/test.txt";
        doReturn(filePath).when(file).toString();

        // when
        String result = splash.toString();

        // then
        assertNotNull(result);
        assertTrue(containsParam(result, "file", filePath));
        assertTrue(containsParam(result, "waitForWindow", String.valueOf(WAIT_FOR_WINDOW)));
        assertTrue(containsParam(result, "timeout", String.valueOf(TIMEOUT)));
        assertTrue(containsParam(result, "timeoutErr", String.valueOf(TIMEOUT_ERR)));
    }
}
