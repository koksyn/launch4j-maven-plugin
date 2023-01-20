package com.akathist.maven.plugins.launch4j;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(JUnitParamsRunner.class)
public class PlatformDetectorTest {
    private PlatformDetector detector;

    @Before
    public void initialize() {
        Log log = mock(Log.class);
        detector = new PlatformDetector(log);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNull_OsName() {
        // expect throws
        detector.detectOS(null, "amd64");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNull_Architecture() {
        // expect throws
        detector.detectOS("Linux", null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowException_ForNotSupportedOS() {
        // expect throws
        detector.detectOS("Unknown", "any");
    }

    @Test
    @Parameters({
        "Windows,any_architecture,win32",
        "Linux,amd64,linux64",
        "Linux,any_other_architecture,linux",
        "Solaris,any_architecture,solaris",
        "SunOS,any_architecture,solaris",
        "Mac OS X,any_architecture,mac",
        "Darwin,any_architecture,mac"
    })
    public void shouldDetectSupportedOS(String osName, String architecture, String expectedDetectedOs) {
        // when
        String detectedOS = detector.detectOS(osName, architecture);

        // then
        assertEquals(expectedDetectedOs, detectedOS);
    }
}
