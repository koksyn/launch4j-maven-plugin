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
    private boolean waitForWindow = true;
    private int timeout = 60;
    private boolean timeoutErr = true;

    // Mocks
    @Mock
    File file;

    // Subject
    private Splash splash;

    @Before
    public void buildFromTestParams() {
        splash = new Splash(file, waitForWindow, timeout, timeoutErr);
    }

    @Test
    public void shouldConvertIntoL4jFormatProperly() {
        // when
        net.sf.launch4j.config.Splash l4jSplash = splash.toL4j();

        // then
        assertNotNull(l4jSplash);
        assertEquals(splash.file, l4jSplash.getFile());
        assertEquals(splash.waitForWindow, l4jSplash.getWaitForWindow());
        assertEquals(splash.timeout, l4jSplash.getTimeout());
        assertEquals(splash.timeoutErr, l4jSplash.isTimeoutErr());
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
        assertTrue(containsParam(result, "waitForWindow", String.valueOf(waitForWindow)));
        assertTrue(containsParam(result, "timeout", String.valueOf(timeout)));
        assertTrue(containsParam(result, "timeoutErr", String.valueOf(timeoutErr)));
    }
}
