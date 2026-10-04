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
    private final com.library.service.SettingService settingService;

    // DANH SÁCH TẤT CẢ PHIẾU MƯỢN (Dành cho Quản lý)
    @GetMapping("/list")
    public String list(
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String statusFilter,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        List<BorrowRecord> allRecords = borrowService.getAll();

        // Thống kê nhanh
        long totalCount = allRecords.size();
        long pendingCount = allRecords.stream().filter(r -> r.getStatus() == BorrowStatus.PENDING).count();
        long borrowingCount = allRecords.stream().filter(r -> r.getStatus() == BorrowStatus.BORROWING).count();
        long overdueCount = allRecords.stream().filter(BorrowRecord::isOverdue).count();
        long returnedCount = allRecords.stream().filter(r -> r.getStatus() == BorrowStatus.RETURNED).count();

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", totalCount);
        stats.put("pending", pendingCount);
        stats.put("borrowing", borrowingCount);
        stats.put("overdue", overdueCount);
        stats.put("returned", returnedCount);

        // Lọc theo trạng thái và từ khóa
        List<BorrowRecord> filteredRecords = allRecords.stream()
                .filter(r -> {
                    if ("PENDING".equalsIgnoreCase(statusFilter)) {
                        return r.getStatus() == BorrowStatus.PENDING;
                    } else if ("BORROWING".equalsIgnoreCase(statusFilter)) {
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
                    boolean matchUser = r.getUser() != null && (
                            (r.getUser().getFullName() != null && r.getUser().getFullName().toLowerCase().contains(k)) ||
                            (r.getUser().getEmail() != null && r.getUser().getEmail().toLowerCase().contains(k)) ||
                            (r.getUser().getUsername() != null && r.getUser().getUsername().toLowerCase().contains(k))
                    );
                    boolean matchDoc = r.getDocument() != null && (
                            (r.getDocument().getTitle() != null && r.getDocument().getTitle().toLowerCase().contains(k)) ||
                            (r.getDocument().getIdentifierCode() != null && r.getDocument().getIdentifierCode().toLowerCase().contains(k))
                    );
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
            model.addAttribute("users", userRepository.findByRole(RoleName.ROLE_READER).stream().filter(User::isActive).toList());
            model.addAttribute("documents", documentRepository.findAll());
            model.addAttribute("borrowDate", request.getBorrowDate() != null ? request.getBorrowDate() : LocalDate.now());
            model.addAttribute("defaultDueDate", request.getDueDate() != null ? request.getDueDate() : LocalDate.now().plusDays(14));
            model.addAttribute("selectedUserId", request.getUserId());
            model.addAttribute("selectedBookId", request.getBookId());
            model.addAttribute("activeMenu", "borrow");
            model.addAttribute("pageTitle", "Lập Phiếu Mượn Tài Liệu");

            return "borrow/create";
        }
    }

    // XỬ LÝ TRẢ SÁCH (Hỗ trợ tiền phạt trễ hạn, phụ phí hư hại và thanh toán Tiền mặt / Mã QR)
    @PostMapping("/{id}/return")
    public String returnBook(
            @PathVariable Long id,
            @ModelAttribute ReturnRequestDto returnDto,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            long totalPayment = returnDto != null ? returnDto.getTotalPayment() : 0L;
            String note = returnDto != null ? returnDto.buildAuditNote() : null;
            String paymentMethod = (totalPayment > 0 && returnDto != null && returnDto.getPaymentMethod() != null)
                    ? returnDto.getPaymentMethod()
                    : "NONE";

            BorrowRecord record = borrowService.returnBook(id, totalPayment, paymentMethod, note);

            String message = "Đã xác nhận trả sách #" + record.getId() + " thành công. Tồn kho tài liệu đã được cập nhật!";
            if (totalPayment > 0) {
                String methodText = "QR_CODE".equalsIgnoreCase(paymentMethod) ? "Mã QR chuyển khoản" : "Tiền mặt";
                message += " (Đã quyết toán " + String.format("%,d", totalPayment) + " VNĐ qua " + methodText + ")";
            }
            redirectAttributes.addFlashAttribute("successMessage", message);

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        String referer = request != null ? request.getHeader("Referer") : null;
        if (referer != null && referer.contains("/overdue")) {
            return "redirect:/borrow/overdue";
        }
        return "redirect:/borrow/list";
    }

    // DANH SÁCH PHIẾU QUÁ HẠN
    @GetMapping("/overdue")
    public String overdue(Model model) {
        List<BorrowRecord> overdueRecords = borrowService.getOverdueRecords();

        long fineRate = settingService != null ? settingService.getFinePerDay() : 5000L;
        long totalEstimatedFine = overdueRecords.stream()
                .mapToLong(r -> r.calculateLateFine(fineRate))
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

        List<BorrowRecord> userHistory = borrowService.getUserHistory(user.getId());
        List<BorrowRecord> pendingBorrows = userHistory.stream()
                .filter(r -> r.getStatus() == BorrowStatus.PENDING && !r.isRequestExpired())
                .toList();

        List<BorrowRecord> activeBorrows = borrowService.getUserBorrowing(user.getId());
        List<BorrowRecord> returnedBorrows = borrowService.getUserReturned(user.getId());

        long activeBorrowCount = activeBorrows.size();
        long overdueCount = activeBorrows.stream().filter(BorrowRecord::isOverdue).count();
        long returnedCount = returnedBorrows.size();
        long pendingCount = pendingBorrows.size();
        long fineRate = settingService != null ? settingService.getFinePerDay() : 5000L;
        long fineAmount = activeBorrows.stream()
                .filter(BorrowRecord::isOverdue)
                .mapToLong(r -> r.calculateLateFine(fineRate))
                .sum();

        model.addAttribute("user", user);
        model.addAttribute("pendingBorrows", pendingBorrows);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("returnedBorrows", returnedBorrows);
        model.addAttribute("activeBorrowCount", activeBorrowCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("returnedCount", returnedCount);
        model.addAttribute("fineAmount", fineAmount);
        model.addAttribute("activeMenu", "history");
        model.addAttribute("pageTitle", "Giỏ Mượn & Sách Tôi Đang Mượn");

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
            String newDueDate = record.getDueDate() != null ?
                    record.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
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

    // Giao diện xác nhận yêu cầu mượn tự phục vụ qua QR
    @GetMapping("/qr-request")
    public String showQrConfirm(
            @RequestParam("bookId") Long bookId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        Document document = documentRepository.findById(bookId).orElse(null);

        if (document == null) {
            model.addAttribute("errorMessage", "Không tìm thấy cuốn sách này trong hệ thống thư viện!");
            return "borrow/qr_confirm";
        }

        // Kiểm tra điều kiện mượn
        boolean canBorrow = true;
        String reason = null;

        if (!document.isBorrowable()) {
            canBorrow = false;
            reason = "Tạp chí chỉ phục vụ đọc tại chỗ trong khuôn viên thư viện, không áp dụng mượn về.";
        } else if (settingService != null && !settingService.isFeatureQrBorrowEnabled()) {
            canBorrow = false;
            reason = "Tính năng mượn sách tự phục vụ qua QR hiện đang tạm dừng để bảo trì hệ thống.";
        } else if (!user.isActive()) {
            canBorrow = false;
            reason = "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ ban quản lý thư viện.";
        }
 else if (document.getQuantity() <= 0 || !document.isAvailable()) {
            canBorrow = false;
            reason = "Cuốn sách này hiện đã hết bản khả dụng trong kho lưu trữ.";
        } else {
            List<BorrowRecord> userRecords = borrowService.getUserHistory(user.getId());
            boolean hasOverdue = userRecords.stream().anyMatch(BorrowRecord::isOverdue);
            if (hasOverdue) {
                canBorrow = false;
                reason = "Bạn đang có tài liệu quá hạn trả. Vui lòng hoàn trả sách tại quầy trước khi mượn tiếp.";
            } else {
                int maxLimit = settingService != null ? settingService.getMaxBorrowLimit() : 5;
                long activeCount = userRecords.stream()
                        .filter(r -> r.getStatus() == BorrowStatus.BORROWING || r.getStatus() == BorrowStatus.OVERDUE || r.getStatus() == BorrowStatus.PENDING)
                        .count();
                if (activeCount >= maxLimit) {
                    canBorrow = false;
                    reason = "Bạn đã đạt giới hạn mượn tối đa (" + maxLimit + " cuốn). Vui lòng trả bớt sách trước khi gửi yêu cầu.";
                } else {
                    boolean alreadyActive = userRecords.stream()
                            .anyMatch(r -> r.getBookId().equals(bookId) && (r.getStatus() == BorrowStatus.BORROWING || r.getStatus() == BorrowStatus.PENDING));
                    if (alreadyActive) {
                        canBorrow = false;
                        reason = "Bạn đã gửi yêu cầu mượn hoặc đang giữ cuốn sách này rồi.";
                    }
                }
            }
        }

        int timeoutMinutes = settingService != null ? settingService.getQrTimeoutMinutes() : 30;
        int maxBorrowDays = settingService != null ? settingService.getMaxBorrowDays() : 14;

        model.addAttribute("document", document);
        model.addAttribute("user", user);
        model.addAttribute("canBorrow", canBorrow);
        model.addAttribute("reason", reason);
        model.addAttribute("timeoutMinutes", timeoutMinutes);
        model.addAttribute("maxBorrowDays", maxBorrowDays);
        model.addAttribute("pageTitle", "Xác Nhận Yêu Cầu Mượn Sách Qua QR");

        return "borrow/qr_confirm";
    }

    // Xử lý gửi yêu cầu mượn tự phục vụ qua QR
    @PostMapping("/qr-request")
    public String submitQrRequest(
            @RequestParam("bookId") Long bookId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        Document document = documentRepository.findById(bookId).orElse(null);
        if (document != null && !document.isBorrowable()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tạp chí chỉ phục vụ đọc tại chỗ trong khuôn viên thư viện, không áp dụng mượn về.");
            return "redirect:/borrow/qr-request?bookId=" + bookId;
        }

        try {
            BorrowRecord record = borrowService.createPendingQrRequest(user.getId(), bookId);
            model.addAttribute("record", record);
            model.addAttribute("document", record.getDocument());
            model.addAttribute("user", user);
            model.addAttribute("timeoutMinutes", settingService != null ? settingService.getQrTimeoutMinutes() : 30);
            return "borrow/qr_success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/borrow/qr-request?bookId=" + bookId;
        }
    }

    // Độc giả tự hủy yêu cầu mượn chờ duyệt
    @PostMapping("/{id}/cancel-request")
    public String cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        try {
            borrowService.cancelPendingRequest(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy yêu cầu mượn sách thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/borrow/history";
    }

    // Màn hình tiếp nhận và duyệt mượn tại quầy chuyển hướng về danh sách lọc Chờ duyệt
    @GetMapping("/requests")
    public String deskRequests() {
        return "redirect:/borrow/list?status=PENDING";
    }

    // Quản lý duyệt mượn tại quầy
    @PostMapping("/requests/{id}/approve")
    public String approveRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "Referer", required = false) String referer,
            RedirectAttributes redirectAttributes) {

        String managerName = (userDetails != null) ? userDetails.getUsername() : "Quản lý";
        if (userDetails instanceof com.library.config.CustomUserDetails cud) {
            managerName = cud.getFullName();
        }

        try {
            BorrowRecord record = borrowService.approvePendingRequest(id, managerName);
            String title = (record.getDocument() != null) ? record.getDocument().getTitle() : "Tài liệu";
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt cho mượn thành công tài liệu: '" + title + "'. Hạn trả: " + record.getDueDate());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể duyệt yêu cầu: " + e.getMessage());
        }

        return "redirect:" + (referer != null ? referer : "/borrow/list?status=PENDING");
    }

    // Quản lý từ chối yêu cầu mượn tại quầy
    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(
            @PathVariable Long id,
            @RequestParam(name = "reason", required = false) String reason,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "Referer", required = false) String referer,
            RedirectAttributes redirectAttributes) {

        String managerName = (userDetails != null) ? userDetails.getUsername() : "Quản lý";
        if (userDetails instanceof com.library.config.CustomUserDetails cud) {
            managerName = cud.getFullName();
        }

        try {
            borrowService.rejectPendingRequest(id, reason, managerName);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu mượn sách và hoàn lại số lượng tồn kho.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:" + (referer != null ? referer : "/borrow/list?status=PENDING");
    }
}