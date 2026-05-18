package com.semanticweb.bookrec.controller;

import com.semanticweb.bookrec.service.GraphVisualizationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Base64;

// Exercise 2 - graph upload
@Controller
@RequestMapping("/graph")
public class GraphController {

    private final GraphVisualizationService graphService;

    public GraphController(GraphVisualizationService graphService) {
        this.graphService = graphService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("chatContext", "general");
        return "graph/upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, Model model) {
        model.addAttribute("chatContext", "general");
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Please choose an RDF/XML file to upload.");
            return "graph/upload";
        }
        try (InputStream in = file.getInputStream()) {
            GraphVisualizationService.RenderedGraph graph = graphService.render(in);
            model.addAttribute("imageBase64", Base64.getEncoder().encodeToString(graph.image()));
            model.addAttribute("triples", graph.triples());
            model.addAttribute("nodes", graph.nodes());
            model.addAttribute("fileName", file.getOriginalFilename());
        } catch (Exception e) {
            model.addAttribute("error", "Could not parse the file as RDF/XML: " + e.getMessage());
        }
        return "graph/upload";
    }
}
