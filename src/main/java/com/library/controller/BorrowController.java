package com.library.controller;

import com.library.dto.BorrowRequestDto;
import com.library.model.BorrowRecord;
import com.library.model.Document;
import com.library.model.User;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import com.library.service.BorrowService;
import com.library.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    // Danh sách phiếu mượn
    @GetMapping("/list")
    public String list(Model model) {
        List<BorrowRecord> borrowRecords = borrowService.getAll();

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
            @ModelAttribute BorrowRequestDto request,
            Model model) {

        try {
            borrowService.create(
                    request.getUserId(),
                    request.getBookId(),
                    request.getBorrowDate(),
                    request.getDueDate());

            return "redirect:/borrow/list";

        } catch (IllegalArgumentException | IllegalStateException e) {

            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("documents", documentRepository.findAll());
            model.addAttribute(
                    "borrowDate",
                    request.getBorrowDate() != null
                            ? request.getBorrowDate()
                            : LocalDate.now());

            return "borrow/create";
        }
    }

    // Xử lý trả sách
    @PostMapping("/{id}/return")
    public String returnBook(@PathVariable Long id) {

        try {
            borrowService.returnBook(id);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Có thể xử lý thông báo lỗi sau
        }

        return "redirect:/borrow/list";
    }

    // Danh sách sách quá hạn
    @GetMapping("/overdue")
    public String overdue(Model model) {

        model.addAttribute(
                "borrowRecords",
                borrowService.getOverdueRecords());

        return "borrow/overdue";
    }

    // Lịch sử mượn của người dùng
    @GetMapping("/history")
    public String viewBorrowHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());

        List<BorrowRecord> activeBorrows = borrowService.getUserBorrowing(user.getId());

        List<BorrowRecord> returnedBorrows = borrowService.getUserReturned(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("returnedBorrows", returnedBorrows);

        return "borrow/history";
    }

    // Top sách được mượn nhiều
    @GetMapping("/top-books")
    public String topBooks(Model model) {

        model.addAttribute(
                "topBooks",
                borrowService.getTopBorrowedBooks());

        return "borrow/top-books";
    }
}