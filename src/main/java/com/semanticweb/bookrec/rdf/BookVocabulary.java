package com.semanticweb.bookrec.rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

// rdf namespace and terms
public final class BookVocabulary {

    private BookVocabulary() {
    }

    public static final String NS = "http://www.semanticweb.org/bookrec#";

    public static final String SPARQL_PREFIX = "PREFIX br: <" + NS + ">\n";

    public static final Resource BOOK = ResourceFactory.createResource(NS + "Book");
    public static final Resource USER = ResourceFactory.createResource(NS + "User");

    public static final Property TITLE = ResourceFactory.createProperty(NS, "title");
    public static final Property HAS_AUTHOR = ResourceFactory.createProperty(NS, "hasAuthor");
    public static final Property HAS_THEME = ResourceFactory.createProperty(NS, "hasTheme");
    public static final Property READING_LEVEL = ResourceFactory.createProperty(NS, "readingLevel");
    public static final Property DESCRIPTION = ResourceFactory.createProperty(NS, "description");

    public static final Property NAME = ResourceFactory.createProperty(NS, "name");
    public static final Property PREFERS_THEME = ResourceFactory.createProperty(NS, "prefersTheme");

    public static String uri(String localName) {
        return NS + localName;
    }
}
