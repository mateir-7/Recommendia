package com.semanticweb.bookrec.controller;

import com.semanticweb.bookrec.model.Book;
import com.semanticweb.bookrec.model.User;
import com.semanticweb.bookrec.service.RdfBookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final RdfBookService bookService;

    public HomeController(RdfBookService bookService) {
        this.bookService = bookService;
    }

    // home page
    @GetMapping("/")
    public String home(Model model) {
        List<User> users = bookService.findAllUsers();
        Map<String, List<Book>> recommendations = new LinkedHashMap<>();
        for (User user : users) {
            recommendations.put(user.getId(), bookService.recommendFor(user.getId()));
        }
        model.addAttribute("users", users);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("bookCount", bookService.findAll().size());
        model.addAttribute("chatContext", "general");
        return "index";
    }
}
