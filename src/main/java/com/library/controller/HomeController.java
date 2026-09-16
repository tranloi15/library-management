package com.library.controller;

import com.library.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DocumentRepository documentRepository;

    @GetMapping({"/", "/home"})
    public String home(Model model,
                       @RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("documents", documentRepository.searchDocuments(keyword.trim(), null, false));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("documents", documentRepository.findAll());
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
