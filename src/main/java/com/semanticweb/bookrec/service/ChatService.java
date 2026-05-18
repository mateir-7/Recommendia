package com.semanticweb.bookrec.service;

import com.semanticweb.bookrec.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Exercise 7 - RAG chat
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int RETRIEVED_DOCUMENTS = 5;

    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;
    private final RdfBookService bookService;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       VectorStoreService vectorStoreService,
                       RdfBookService bookService) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStoreService = vectorStoreService;
        this.bookService = bookService;
    }

    public String answer(String message, String contextType, String bookId) {
        if (message == null || message.isBlank()) {
            return "Please type a question about the library.";
        }

        String retrievalQuery = message;
        if ("book-detail".equals(contextType) && bookId != null && !bookId.isBlank()) {
            Optional<Book> current = bookService.findById(bookId);
            if (current.isPresent()) {
                retrievalQuery = current.get().getTitle() + " - " + message;
            }
        }

        List<Document> retrieved = vectorStoreService.search(retrievalQuery, RETRIEVED_DOCUMENTS);
        String libraryContext = retrieved.isEmpty()
                ? "(no matching records found in the library)"
                : retrieved.stream().map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));

        String systemPrompt = """
                You are the friendly assistant of an online book library.
                Answer ONLY using the LIBRARY CONTEXT below. This context is retrieved
                from the library's own database and is the single source of truth:
                trust it even if it contradicts your own knowledge. For example, if the
                context says a book was written by a certain author, use that author.
                If the LIBRARY CONTEXT does not contain the answer, say that the library
                has no information about it. Keep answers short and helpful.

                LIBRARY CONTEXT:
                %s
                """.formatted(libraryContext);

        try {
            String reply = chatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .content();
            return (reply == null || reply.isBlank()) ? "(no response from the model)" : reply;
        } catch (Exception e) {
            log.error("Chat completion failed", e);
            return "Sorry, the chat service is currently unavailable. "
                    + "Please make sure the OPENROUTER_API_KEY environment variable is set "
                    + "with a valid key. (" + e.getMessage() + ")";
        }
    }

    // context-aware conversation starters
    public List<String> conversationStarters(String contextType, String bookId) {
        if ("book-detail".equals(contextType) && bookId != null && !bookId.isBlank()) {
            Optional<Book> current = bookService.findById(bookId);
            if (current.isPresent()) {
                String title = current.get().getTitle();
                return List.of(
                        "Tell me more about \"" + title + "\".",
                        "Who is the author of \"" + title + "\"?",
                        "What genre is \"" + title + "\" and which readers would enjoy it?");
            }
        }
        if ("book-list".equals(contextType)) {
            return List.of(
                    "What book am I most likely to enjoy from this list?",
                    "Which books are suitable for a Beginner reader?",
                    "Recommend a Science Fiction book from the library.");
        }
        return List.of(
                "What book has the author Frank Herbert and the theme Science Fiction?",
                "Which books would you recommend for Alice?",
                "What genres are available in the library?");
    }
}
