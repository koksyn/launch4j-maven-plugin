package com.akathist.maven.plugins.launch4j.utils;

public class ToStringVerifier {
    private ToStringVerifier() {
    }

    public static boolean containsParam(String subject, String paramName, String paramValue) {
        if(subject == null) {
            throw new IllegalArgumentException("Subject is null.");
        }

        return subject.contains(paramName + "='" + paramValue + "'");
    }
}
