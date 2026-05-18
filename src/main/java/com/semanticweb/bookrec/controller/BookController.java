package com.semanticweb.bookrec.controller;

import com.semanticweb.bookrec.model.Book;
import com.semanticweb.bookrec.service.RdfBookService;
import com.semanticweb.bookrec.service.VectorStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

// Exercise 3 & 4 - book pages
@Controller
@RequestMapping("/books")
public class BookController {

    private final RdfBookService bookService;
    private final VectorStoreService vectorStoreService;

    public BookController(RdfBookService bookService, VectorStoreService vectorStoreService) {
        this.bookService = bookService;
        this.vectorStoreService = vectorStoreService;
    }

    // list books
    @GetMapping
    public String list(Model model) {
        model.addAttribute("books", bookService.findAll());
        model.addAttribute("chatContext", "book-list");
        return "books/list";
    }

    // add form
    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("book", new Book("", "", List.of(), List.of(), "", ""));
        model.addAttribute("mode", "add");
        model.addAttribute("chatContext", "general");
        return "books/form";
    }

    // edit form
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        Book book = bookService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        model.addAttribute("book", book);
        model.addAttribute("mode", "edit");
        model.addAttribute("chatContext", "general");
        return "books/form";
    }

    // save book
    @PostMapping("/save")
    public String save(@RequestParam(required = false) String id,
                        @RequestParam String title,
                        @RequestParam(required = false) String authors,
                        @RequestParam(required = false) String themes,
                        @RequestParam String readingLevel,
                        @RequestParam(required = false) String description,
                        RedirectAttributes redirectAttributes) {
        if (title == null || title.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Title is required.");
            return "redirect:/books/add";
        }
        String bookId = (id != null && !id.isBlank()) ? id : RdfBookService.slug(title);
        Book book = new Book(bookId, title.trim(),
                splitCsv(authors), splitCsv(themes), readingLevel, description);
        bookService.save(book);
        vectorStoreService.reindex();
        redirectAttributes.addFlashAttribute("message", "Saved \"" + book.getTitle() + "\".");
        return "redirect:/books/" + bookId;
    }

    // one book page
    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        Book book = bookService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        model.addAttribute("book", book);
        model.addAttribute("chatContext", "book-detail");
        model.addAttribute("chatBookId", id);
        return "books/detail";
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
