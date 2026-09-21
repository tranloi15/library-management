package com.library.controller;

import com.library.model.BorrowRecord;
import com.library.model.Document;
import com.library.model.User;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import com.library.service.BorrowRecordService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowRecordController {

    private final BorrowRecordService borrowRecordService;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    // Trang danh sách phiếu mượn
    @GetMapping("/list")
    public String list(Model model) {

        List<BorrowRecord> borrowRecords = borrowRecordService.getAll();

        model.addAttribute("borrowRecords", borrowRecords);

        return "borrow/list";
    }

    // Trang tạo phiếu mượn
    @GetMapping("/create")
    public String createForm(Model model) {

        List<User> users = userRepository.findAll();
        List<Document> documents = documentRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("documents", documents);
        model.addAttribute("borrowDate", LocalDate.now());

        return "borrow/create";
    }

    // Xử lý tạo phiếu mượn
    @PostMapping("/create")
    public String create(
            @RequestParam Long userId,
            @RequestParam Long bookId,
            @RequestParam(required = false) LocalDate borrowDate,
            @RequestParam LocalDate dueDate,
            Model model) {

        try {

            borrowRecordService.create(
                    userId,
                    bookId,
                    borrowDate,
                    dueDate);

            return "redirect:/borrow/list";

        } catch (IllegalArgumentException | IllegalStateException e) {

            model.addAttribute("error", e.getMessage());

            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("documents", documentRepository.findAll());
            model.addAttribute("borrowDate",
                    borrowDate != null ? borrowDate : LocalDate.now());

            return "borrow/create";
        }
    }

    // Xử lý trả sách
    @PostMapping("/{id}/return")
    public String returnBook(
            @PathVariable Long id,
            Model model) {

        try {

            borrowRecordService.returnBook(id);

        } catch (IllegalArgumentException | IllegalStateException e) {

            model.addAttribute("error", e.getMessage());
        }

        return "redirect:/borrow/list";
    }

    @GetMapping("/overdue")
    public String overdue(Model model) {

        model.addAttribute(
                "borrowRecords",
                borrowRecordService.getOverdueRecords());

        return "borrow/overdue";
    }

    @GetMapping("/top-books")
    public String topBooks(Model model) {

        model.addAttribute(
                "topBooks",
                borrowRecordService.getTopBorrowedBooks());

        return "borrow/top-books";
    }
}