package com.akathist.maven.plugins.launch4j;

import org.apache.maven.plugin.logging.Log;

class PlatformDetector {
    private Log log;

    PlatformDetector(Log log) {
        this.log = log;
    }

    String detectOSFromSystemProperties() {
        String osName = System.getProperty("os.name");
        log.debug("OS = " + osName);

        String architecture = System.getProperty("os.arch");
        log.debug("Architecture = " + architecture);

        return detectOS(osName, architecture);
    }

    /**
     * See here for possible values of osName:
     * @see http://lopica.sourceforge.net/os.html
     */
    String detectOS(String osName, String architecture) {
        if(osName == null) {
            throw new IllegalArgumentException("osName cannot be null");
        }
        if(architecture == null) {
            throw new IllegalArgumentException("architecture cannot be null");
        }

        if (osName.startsWith("Windows")) {
            return "win32";
        } else if ("Linux".equals(osName)) {
            if ("amd64".equals(architecture)) {
                return "linux64";
            } else {
                return "linux";
            }
        } else if ("Solaris".equals(osName) || "SunOS".equals(osName)) {
            return "solaris";
        } else if ("Mac OS X".equals(osName) || "Darwin".equals(osName)) {
            return "mac";
        } else {
            throw new IllegalStateException("Sorry, Launch4j doesn't support the '" + osName + "' OS.");
        }
    }
}
