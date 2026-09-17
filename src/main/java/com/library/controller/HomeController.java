package com.library.controller;

import com.library.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DocumentService documentService;

    @GetMapping({"/", "/home"})
    public String home(Model model,
                       @RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("documents", documentService.searchDocuments(keyword.trim()));
            model.addAttribute("keyword", keyword.trim());
        } else {
            model.addAttribute("documents", documentService.getAllDocuments());
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
