package com.akathist.maven.plugins.launch4j;

import net.sf.launch4j.config.Msg;
import org.junit.Before;
import org.junit.Test;

import static com.akathist.maven.plugins.launch4j.utils.ToStringVerifier.containsParam;
import static org.junit.Assert.*;

public class MessagesTest {
    // Params
    private final String STARTUP_ERR = "Mutex-1337";
    private final String JRE_VERSION_ERR = "Mutex-1337";
    private final String LAUNCHER_ERR = "Mutex-1337";
    private final String INSTANCE_ALREADY_EXISTS_MSG = "Mutex-1337";
    private final String JRE_NOT_FOUND_ERR = "Mutex-1337";

    // Subject
    private Messages messages;

    @Before
    public void buildFromTestParams() {
        messages = new Messages(STARTUP_ERR, null,
                JRE_VERSION_ERR, LAUNCHER_ERR,
                INSTANCE_ALREADY_EXISTS_MSG, JRE_NOT_FOUND_ERR
        );
    }

    @Test
    public void shouldConvertIntoL4jFormatProperly() {
        // when
        Msg l4jMessages = messages.toL4j();

        // then
        assertNotNull(l4jMessages);
        assertEquals(STARTUP_ERR, l4jMessages.getStartupErr());
        assertEquals(JRE_VERSION_ERR, l4jMessages.getJreVersionErr());
        assertEquals(LAUNCHER_ERR, l4jMessages.getLauncherErr());
        assertEquals(INSTANCE_ALREADY_EXISTS_MSG, l4jMessages.getInstanceAlreadyExistsMsg());
        assertEquals(JRE_NOT_FOUND_ERR, l4jMessages.getJreNotFoundErr());
    }

    @Test
    public void shouldGenerateString_WithTestParams() {
        // when
        String result = messages.toString();

        // then
        assertNotNull(result);
        assertTrue(containsParam(result, "startupErr", STARTUP_ERR));
        assertTrue(containsParam(result, "jreVersionErr", JRE_VERSION_ERR));
        assertTrue(containsParam(result, "launcherErr", LAUNCHER_ERR));
        assertTrue(containsParam(result, "instanceAlreadyExistsMsg", INSTANCE_ALREADY_EXISTS_MSG));
        assertTrue(containsParam(result, "jreNotFoundErr", JRE_NOT_FOUND_ERR));
    }
}
