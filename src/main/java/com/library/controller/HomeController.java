package com.library.controller;

import com.library.model.User;
import com.library.service.UserService;
import com.library.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DocumentService documentService;
    private final UserService userService;

    @GetMapping({"/", "/home"})
    public String home(Model model,
                       @RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return "redirect:/catalog?keyword=" + java.net.URLEncoder.encode(keyword.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }
        model.addAttribute("documents", documentService.getAllDocuments());
        return "home";
    }

    @GetMapping("/catalog")
    public String catalog(Model model,
                          @RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "type", required = false) String type,
                          @RequestParam(value = "view", required = false) String view) {
        var allDocs = (keyword != null && !keyword.trim().isEmpty())
                ? documentService.searchDocuments(keyword.trim())
                : documentService.getAllDocuments();

        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            allDocs = allDocs.stream()
                    .filter(d -> d.getDocumentType() != null && d.getDocumentType().name().equalsIgnoreCase(type.trim()))
                    .toList();
            model.addAttribute("selectedType", type.trim().toUpperCase());
        } else {
            model.addAttribute("selectedType", "ALL");
        }

        model.addAttribute("documents", allDocs);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("view", (view != null && !view.trim().isEmpty()) ? view.trim() : "grid");
        return "catalog";
    }

    @GetMapping("/login")
    public String login(jakarta.servlet.http.HttpServletRequest request) {
        org.springframework.security.web.csrf.CsrfToken csrf =
            (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        if (csrf != null) {
            csrf.getToken(); // Eagerly initialize token and session before template commits response
        }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "redirect:/login?action=register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam("fullName") String fullName,
                                  @RequestParam("email") String email,
                                  @RequestParam("password") String password) {
        try {
            User newUser = new User();
            newUser.setFullName(fullName.trim());
            newUser.setEmail(email.trim().toLowerCase());
            newUser.setPassword(password);
            userService.createReader(newUser);
            return "redirect:/login?regSuccess=1&action=register";
        } catch (IllegalArgumentException e) {
            String errMsg = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/login?regError=" + errMsg + "&action=register";
        }
    }
}
