package com.semanticweb.bookrec.service;

import com.semanticweb.bookrec.model.Book;
import com.semanticweb.bookrec.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Exercise 7 - in-memory vector database
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private record Entry(Document document, float[] vector) {
    }

    private final EmbeddingModel embeddingModel;
    private final RdfBookService bookService;
    private final List<Entry> store = new ArrayList<>();

    public VectorStoreService(EmbeddingModel embeddingModel, RdfBookService bookService) {
        this.embeddingModel = embeddingModel;
        this.bookService = bookService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        reindex();
    }

    // rebuild the index
    public synchronized void reindex() {
        try {
            List<Entry> rebuilt = new ArrayList<>();
            for (Book book : bookService.findAll()) {
                Document doc = bookDocument(book);
                rebuilt.add(new Entry(doc, embeddingModel.embed(doc.getText())));
            }
            for (User user : bookService.findAllUsers()) {
                Document doc = userDocument(user);
                rebuilt.add(new Entry(doc, embeddingModel.embed(doc.getText())));
            }
            store.clear();
            store.addAll(rebuilt);
            log.info("Vector store indexed {} documents", store.size());
        } catch (Exception e) {
            log.error("Vector store indexing failed - the chatbot RAG will be degraded", e);
        }
    }

    public synchronized List<Document> search(String query, int topK) {
        if (store.isEmpty()) {
            return List.of();
        }
        float[] queryVector = embeddingModel.embed(query);
        return store.stream()
                .sorted(Comparator.comparingDouble(
                        (Entry e) -> cosineSimilarity(queryVector, e.vector())).reversed())
                .limit(topK)
                .map(Entry::document)
                .toList();
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length && i < b.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Document bookDocument(Book book) {
        String text = "BOOK: " + book.getTitle() + "\n"
                + "Author(s): " + book.getAuthorsText() + "\n"
                + "Theme(s) / genre: " + book.getThemesText() + "\n"
                + "Reading level: " + book.getReadingLevel() + "\n"
                + "Description: " + book.getDescription();
        return Document.builder()
                .id("book:" + book.getId())
                .text(text)
                .metadata(Map.<String, Object>of(
                        "type", "book", "id", book.getId(), "title", book.getTitle()))
                .build();
    }

    private Document userDocument(User user) {
        String text = "USER PROFILE: " + user.getName() + "\n"
                + "Reading level: " + user.getReadingLevel() + "\n"
                + "Preferred theme / genre: " + user.getPreferredTheme();
        return Document.builder()
                .id("user:" + user.getId())
                .text(text)
                .metadata(Map.<String, Object>of(
                        "type", "user", "id", user.getId(), "name", user.getName()))
                .build();
    }
}
