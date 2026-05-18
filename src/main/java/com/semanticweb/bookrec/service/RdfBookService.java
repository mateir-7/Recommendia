package com.semanticweb.bookrec.service;

import com.semanticweb.bookrec.model.Book;
import com.semanticweb.bookrec.model.User;
import com.semanticweb.bookrec.rdf.BookVocabulary;
import jakarta.annotation.PostConstruct;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Exercise 3 & 4 - reads and writes books with jena
@Service
public class RdfBookService {

    private static final Logger log = LoggerFactory.getLogger(RdfBookService.class);

    @Value("${app.rdf.data-file}")
    private String dataFilePath;

    @Value("${app.rdf.seed}")
    private org.springframework.core.io.Resource seedResource;

    private Path dataFile;
    private Model model;

    @PostConstruct
    synchronized void init() {
        try {
            dataFile = Path.of(dataFilePath);
            if (Files.notExists(dataFile)) {
                if (dataFile.getParent() != null) {
                    Files.createDirectories(dataFile.getParent());
                }
                try (InputStream in = seedResource.getInputStream()) {
                    Files.copy(in, dataFile);
                }
                log.info("Seeded RDF data store at {}", dataFile.toAbsolutePath());
            }
            model = ModelFactory.createDefaultModel();
            try (InputStream in = Files.newInputStream(dataFile)) {
                model.read(in, "", "RDF/XML");
            }
            log.info("Loaded RDF model: {} statements", model.size());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize the RDF data store", e);
        }
    }

    public synchronized List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        ResIterator it = model.listSubjectsWithProperty(RDF.type, BookVocabulary.BOOK);
        while (it.hasNext()) {
            books.add(toBook(it.nextResource()));
        }
        books.sort(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
        return books;
    }

    public synchronized Optional<Book> findById(String id) {
        Resource r = model.getResource(BookVocabulary.uri(id));
        if (!model.contains(r, RDF.type, BookVocabulary.BOOK)) {
            return Optional.empty();
        }
        return Optional.of(toBook(r));
    }

    public synchronized boolean exists(String id) {
        Resource r = model.getResource(BookVocabulary.uri(id));
        return model.contains(r, RDF.type, BookVocabulary.BOOK);
    }

    public synchronized List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        ResIterator it = model.listSubjectsWithProperty(RDF.type, BookVocabulary.USER);
        while (it.hasNext()) {
            users.add(toUser(it.nextResource()));
        }
        users.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        return users;
    }

    public synchronized Optional<User> findUser(String id) {
        Resource r = model.getResource(BookVocabulary.uri(id));
        if (!model.contains(r, RDF.type, BookVocabulary.USER)) {
            return Optional.empty();
        }
        return Optional.of(toUser(r));
    }

    // recommendation rule as a sparql query
    public synchronized List<Book> recommendFor(String userId) {
        Resource user = model.getResource(BookVocabulary.uri(userId));
        if (!model.contains(user, RDF.type, BookVocabulary.USER)) {
            return List.of();
        }
        String sparql = BookVocabulary.SPARQL_PREFIX
                + "SELECT DISTINCT ?book WHERE { "
                + "  ?book a br:Book ; br:hasTheme ?theme ; br:readingLevel ?level . "
                + "  <" + user.getURI() + "> br:prefersTheme ?theme ; br:readingLevel ?level . "
                + "}";
        List<Book> result = new ArrayList<>();
        Query query = QueryFactory.create(sparql);
        try (QueryExecution exec = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = exec.execSelect();
            while (rs.hasNext()) {
                result.add(toBook(rs.next().getResource("book")));
            }
        }
        result.sort(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    // Exercise 3 - add or update a book
    public synchronized Book save(Book book) {
        Resource r = model.getResource(BookVocabulary.uri(book.getId()));
        model.removeAll(r, null, null);

        r.addProperty(RDF.type, BookVocabulary.BOOK);
        r.addProperty(BookVocabulary.TITLE, book.getTitle());
        for (String author : book.getAuthors()) {
            r.addProperty(BookVocabulary.HAS_AUTHOR, author);
        }
        for (String theme : book.getThemes()) {
            r.addProperty(BookVocabulary.HAS_THEME, theme);
        }
        r.addProperty(BookVocabulary.READING_LEVEL, book.getReadingLevel());
        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            r.addProperty(BookVocabulary.DESCRIPTION, book.getDescription());
        }
        persist();
        return toBook(r);
    }

    private void persist() {
        try (OutputStream out = Files.newOutputStream(dataFile)) {
            model.write(out, "RDF/XML-ABBREV");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save RDF data file", e);
        }
    }

    private Book toBook(Resource r) {
        return new Book(
                localName(r),
                literal(r, BookVocabulary.TITLE),
                literals(r, BookVocabulary.HAS_AUTHOR),
                literals(r, BookVocabulary.HAS_THEME),
                literal(r, BookVocabulary.READING_LEVEL),
                literal(r, BookVocabulary.DESCRIPTION));
    }

    private User toUser(Resource r) {
        return new User(
                localName(r),
                literal(r, BookVocabulary.NAME),
                literal(r, BookVocabulary.READING_LEVEL),
                literal(r, BookVocabulary.PREFERS_THEME));
    }

    private String localName(Resource r) {
        return r.getURI().substring(BookVocabulary.NS.length());
    }

    private String literal(Resource r, Property p) {
        Statement st = r.getProperty(p);
        return (st != null && st.getObject().isLiteral()) ? st.getString() : "";
    }

    private List<String> literals(Resource r, Property p) {
        List<String> values = new ArrayList<>();
        StmtIterator it = r.listProperties(p);
        while (it.hasNext()) {
            Statement st = it.nextStatement();
            if (st.getObject().isLiteral()) {
                values.add(st.getString());
            }
        }
        return values;
    }

    public static String slug(String title) {
        String s = title.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return s.isEmpty() ? "book" : s;
    }
}
