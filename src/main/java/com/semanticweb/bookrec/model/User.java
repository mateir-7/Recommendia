package com.semanticweb.bookrec.model;

public class User {

    private final String id;
    private final String name;
    private final String readingLevel;
    private final String preferredTheme;

    public User(String id, String name, String readingLevel, String preferredTheme) {
        this.id = id;
        this.name = name;
        this.readingLevel = readingLevel;
        this.preferredTheme = preferredTheme;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getReadingLevel() {
        return readingLevel;
    }

    public String getPreferredTheme() {
        return preferredTheme;
    }
}
