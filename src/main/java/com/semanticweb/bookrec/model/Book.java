package com.semanticweb.bookrec.model;

import java.util.ArrayList;
import java.util.List;

public class Book {

    private final String id;
    private final String title;
    private final List<String> authors;
    private final List<String> themes;
    private final String readingLevel;
    private final String description;

    public Book(String id, String title, List<String> authors, List<String> themes,
                String readingLevel, String description) {
        this.id = id;
        this.title = title;
        this.authors = authors == null ? new ArrayList<>() : authors;
        this.themes = themes == null ? new ArrayList<>() : themes;
        this.readingLevel = readingLevel;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public List<String> getThemes() {
        return themes;
    }

    public String getReadingLevel() {
        return readingLevel;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthorsText() {
        return String.join(", ", authors);
    }

    public String getThemesText() {
        return String.join(", ", themes);
    }
}
