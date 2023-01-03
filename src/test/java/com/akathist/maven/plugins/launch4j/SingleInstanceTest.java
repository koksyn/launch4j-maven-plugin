package com.akathist.maven.plugins.launch4j;

import org.junit.Before;
import org.junit.Test;

import static com.akathist.maven.plugins.launch4j.utils.ToStringVerifier.containsParam;
import static org.junit.Assert.*;

public class SingleInstanceTest {
    // Params
    private final String MUTEX_NAME = "Mutex-1337";
    private final String WINDOW_TITLE = "Hello world";

    // Subject
    private SingleInstance singleInstance;

    @Before
    public void buildFromTestParams() {
        singleInstance = new SingleInstance(MUTEX_NAME, WINDOW_TITLE);
    }

    @Test
    public void shouldConvertIntoL4jFormatProperly() {
        // when
        net.sf.launch4j.config.SingleInstance l4jSingleInstance = singleInstance.toL4j();

        // then
        assertNotNull(l4jSingleInstance);
        assertEquals(MUTEX_NAME, l4jSingleInstance.getMutexName());
        assertEquals(WINDOW_TITLE, l4jSingleInstance.getWindowTitle());
    }

    @Test
    public void shouldGenerateString_WithTestParams() {
        // when
        String result = singleInstance.toString();

        // then
        assertNotNull(result);
        assertTrue(containsParam(result, "mutexName", MUTEX_NAME));
        assertTrue(containsParam(result, "windowTitle", WINDOW_TITLE));
    }
}
