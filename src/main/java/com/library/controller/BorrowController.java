package com.library.controller;

import com.library.dto.BorrowRequestDto;
import com.library.dto.ReturnRequestDto;
import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.model.RoleName;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    // DANH SÁCH TẤT CẢ PHIẾU MƯỢN (Dành cho Quản lý)
    @GetMapping("/list")
    public String list(
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String statusFilter,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        List<BorrowRecord> allRecords = borrowService.getAll();

        // Thống kê nhanh
        long totalCount = allRecords.size();
        long borrowingCount = allRecords.stream().filter(r -> r.getStatus() == BorrowStatus.BORROWING).count();
        long overdueCount = allRecords.stream().filter(BorrowRecord::isOverdue).count();
        long returnedCount = allRecords.stream().filter(r -> r.getStatus() == BorrowStatus.RETURNED).count();

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", totalCount);
        stats.put("borrowing", borrowingCount);
        stats.put("overdue", overdueCount);
        stats.put("returned", returnedCount);

        // Lọc theo trạng thái và từ khóa
        List<BorrowRecord> filteredRecords = allRecords.stream()
                .filter(r -> {
                    if ("BORROWING".equalsIgnoreCase(statusFilter)) {
                        return r.getStatus() == BorrowStatus.BORROWING;
                    } else if ("OVERDUE".equalsIgnoreCase(statusFilter)) {
                        return r.isOverdue();
                    } else if ("RETURNED".equalsIgnoreCase(statusFilter)) {
                        return r.getStatus() == BorrowStatus.RETURNED;
                    }
                    return true;
                })
                .filter(r -> {
                    if (keyword == null || keyword.trim().isEmpty()) {
                        return true;
                    }
                    String k = keyword.trim().toLowerCase();
                    boolean matchId = String.valueOf(r.getId()).contains(k) || ("#br-" + r.getId()).contains(k);
                    boolean matchUser = r.getUser() != null && ((r.getUser().getFullName() != null
                            && r.getUser().getFullName().toLowerCase().contains(k)) ||
                            (r.getUser().getEmail() != null && r.getUser().getEmail().toLowerCase().contains(k)) ||
                            (r.getUser().getUsername() != null && r.getUser().getUsername().toLowerCase().contains(k)));
                    boolean matchDoc = r.getDocument() != null && ((r.getDocument().getTitle() != null
                            && r.getDocument().getTitle().toLowerCase().contains(k)) ||
                            (r.getDocument().getIdentifierCode() != null
                                    && r.getDocument().getIdentifierCode().toLowerCase().contains(k)));
                    return matchId || matchUser || matchDoc;
                })
                .toList();

        model.addAttribute("borrowRecords", filteredRecords);
        model.addAttribute("stats", stats);
        model.addAttribute("currentStatus", statusFilter);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("activeMenu", "borrow");
        model.addAttribute("pageTitle", "Quản Lý Mượn - Trả Tài Liệu");

        return "borrow/list";
    }

    // TRANG LẬP PHIẾU MƯỢN MỚI
    @GetMapping("/create")
    public String createForm(Model model) {
        // Chỉ lấy các tài khoản Độc giả (ROLE_READER) đang hoạt động
        List<User> activeReaders = userRepository.findByRole(RoleName.ROLE_READER).stream()
                .filter(User::isActive)
                .toList();

        // Lấy tất cả tài liệu kèm thông tin tồn kho
        List<Document> documents = documentRepository.findAll();

        model.addAttribute("users", activeReaders);
        model.addAttribute("documents", documents);
        model.addAttribute("borrowDate", LocalDate.now());
        model.addAttribute("defaultDueDate", LocalDate.now().plusDays(14)); // Mặc định 14 ngày
        model.addAttribute("activeMenu", "borrow");
        model.addAttribute("pageTitle", "Lập Phiếu Mượn Tài Liệu");

        return "borrow/create";
    }

    // XỬ LÝ TẠO PHIẾU MƯỢN
    @PostMapping("/create")
    public String create(
            @ModelAttribute BorrowRequestDto request,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            BorrowRecord createdRecord = borrowService.create(
                    request.getUserId(),
                    request.getBookId(),
                    request.getBorrowDate(),
                    request.getDueDate());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã lập phiếu mượn #" + createdRecord.getId() + " thành công cho độc giả " +
                            (createdRecord.getUser() != null ? createdRecord.getUser().getFullName() : "") + "!");
            return "redirect:/borrow/list";

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("users",
                    userRepository.findByRole(RoleName.ROLE_READER).stream().filter(User::isActive).toList());
            model.addAttribute("documents", documentRepository.findAll());
            model.addAttribute("borrowDate",
                    request.getBorrowDate() != null ? request.getBorrowDate() : LocalDate.now());
            model.addAttribute("defaultDueDate",
                    request.getDueDate() != null ? request.getDueDate() : LocalDate.now().plusDays(14));
            model.addAttribute("selectedUserId", request.getUserId());
            model.addAttribute("selectedBookId", request.getBookId());
            model.addAttribute("activeMenu", "borrow");
            model.addAttribute("pageTitle", "Lập Phiếu Mượn Tài Liệu");

            return "borrow/create";
        }
    }

    // XỬ LÝ TRẢ SÁCH (Hỗ trợ tiền phạt và thanh toán Tiền mặt / Mã QR)
    @PostMapping("/{id}/return")
    public String returnBook(
            @PathVariable Long id,
            @ModelAttribute ReturnRequestDto request,
            RedirectAttributes redirectAttributes) {

        try {
            BorrowRecord currentRecord = borrowService.getById(id);

            long fineAmount = currentRecord.calculateLateFine(5000L);

            borrowService.returnBook(
                    id,
                    fineAmount,
                    request.getDamageFee(),
                    request.getPaymentMethod(),
                    request.getNote(),
                    request.getBookCondition());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Trả sách thành công");

        } catch (IllegalArgumentException | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/borrow/list";
    }

    // DANH SÁCH PHIẾU QUÁ HẠN
    @GetMapping("/overdue")
    public String overdue(Model model) {
        List<BorrowRecord> overdueRecords = borrowService.getOverdueRecords();

        long totalEstimatedFine = overdueRecords.stream()
                .mapToLong(r -> r.calculateLateFine(5000L))
                .sum();

        model.addAttribute("borrowRecords", overdueRecords);
        model.addAttribute("overdueCount", overdueRecords.size());
        model.addAttribute("totalEstimatedFine", totalEstimatedFine);
        model.addAttribute("activeMenu", "borrow");
        model.addAttribute("pageTitle", "Danh Sách Phiếu Mượn Quá Hạn");

        return "borrow/overdue";
    }

    // XẾP HẠNG TOP TÀI LIỆU ĐƯỢC MƯỢN NHIỀU NHẤT
    @GetMapping("/top-books")
    public String topBooks(Model model) {
        List<Object[]> topBooks = borrowService.getTopBorrowedBooks();

        model.addAttribute("topBooks", topBooks);
        model.addAttribute("activeMenu", "borrow");
        model.addAttribute("pageTitle", "Xếp Hạng Tài Liệu Mượn Nhiều Nhất");

        return "borrow/top-books";
    }

    // LỊCH SỬ MƯỢN CỦA ĐỘC GIẢ ĐANG ĐĂNG NHẬP
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

        long activeBorrowCount = activeBorrows.size();
        long overdueCount = activeBorrows.stream().filter(BorrowRecord::isOverdue).count();
        long returnedCount = returnedBorrows.size();
        long fineAmount = activeBorrows.stream()
                .filter(BorrowRecord::isOverdue)
                .mapToLong(r -> r.calculateLateFine(5000L))
                .sum();

        model.addAttribute("user", user);
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("returnedBorrows", returnedBorrows);
        model.addAttribute("activeBorrowCount", activeBorrowCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("returnedCount", returnedCount);
        model.addAttribute("fineAmount", fineAmount);
        model.addAttribute("activeMenu", "history");
        model.addAttribute("pageTitle", "Sách Tôi Đang Mượn & Lịch Sử");

        return "borrow/history";
    }

    // XỬ LÝ GIA HẠN THỜI HẠN MƯỢN SÁCH
    @PostMapping("/{id}/renew")
    public String renewBook(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        boolean isAdmin = user.getRole() == RoleName.ROLE_ADMIN;

        try {
            BorrowRecord record = borrowService.renewBorrow(id, user.getId(), isAdmin);
            String docTitle = (record.getDocument() != null) ? record.getDocument().getTitle() : "Tài liệu";
            String newDueDate = record.getDueDate() != null
                    ? record.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Gia hạn thành công sách '" + docTitle + "' thêm 7 ngày! Hạn trả mới: " + newDueDate);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        if (isAdmin) {
            return "redirect:/borrow/list";
        }
        return "redirect:/borrow/history";
    }
}