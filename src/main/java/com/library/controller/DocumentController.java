package com.library.controller;

import com.library.dto.BookFormDto;
import com.library.dto.MagazineFormDto;
import com.library.enums.DocumentType;
import com.library.model.Document;
import com.library.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    // 1. Xem danh sách tài liệu kèm bộ lọc
    @GetMapping
    public String listDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DocumentType docType,
            @RequestParam(defaultValue = "false") boolean onlyInStock,
            Model model) {
        List<Document> documents = documentService.searchDocuments(keyword, docType, onlyInStock);
        model.addAttribute("documents", documents);
        model.addAttribute("keyword", keyword);
        model.addAttribute("docType", docType);
        model.addAttribute("onlyInStock", onlyInStock);
        return "documents/list";
    }

    // 2. Thêm mới Sách
    @GetMapping("/add-book")
    public String showAddBookForm(Model model) {
        model.addAttribute("bookForm", new BookFormDto());
        return "documents/add_book";
    }

    @PostMapping("/add-book")
    public String addBook(
            @Valid @ModelAttribute("bookForm") BookFormDto bookForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "documents/add_book";
        }
        try {
            documentService.saveBook(bookForm);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới sách thành công!");
            return "redirect:/documents";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("isbn", "error.bookForm", e.getMessage());
            return "documents/add_book";
        }
    }

    // 3. Thêm mới Tạp chí
    @GetMapping("/add-magazine")
    public String showAddMagazineForm(Model model) {
        model.addAttribute("magazineForm", new MagazineFormDto());
        return "documents/add_magazine";
    }

    @PostMapping("/add-magazine")
    public String addMagazine(
            @Valid @ModelAttribute("magazineForm") MagazineFormDto magazineForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "documents/add_magazine";
        }
        documentService.saveMagazine(magazineForm);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới tạp chí thành công!");
        return "redirect:/documents";
    }

    // 4. Xóa tài liệu
    @PostMapping("/delete/{id}")
    public String deleteDocument(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            documentService.deleteDocument(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài liệu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/documents";
    }
}