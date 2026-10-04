package com.library.controller;

import com.library.model.BorrowRecord;
import com.library.model.Document;
import com.library.model.User;
import com.library.service.BorrowCartService;
import com.library.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/borrow/cart")
@RequiredArgsConstructor
public class BorrowCartController {

    private final BorrowCartService borrowCartService;
    private final UserService userService;

    // Xem giỏ mượn cá nhân
    @GetMapping
    public String viewCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpSession session,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        List<Document> cartItems = borrowCartService.getCartItems(session);
        int cartCount = cartItems.size();
        int activeCount = borrowCartService.getActiveBorrowCount(user);
        int availableQuota = borrowCartService.getAvailableQuota(user);

        model.addAttribute("user", user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("availableQuota", availableQuota);
        model.addAttribute("activeMenu", "cart");
        model.addAttribute("pageTitle", "Giỏ Mượn Sách Cá Nhân");

        return "borrow/cart";
    }

    // AJAX thêm tài liệu vào giỏ
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestParam("bookId") Long bookId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpSession session) {

        if (userDetails == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", "Vui lòng đăng nhập để thêm tài liệu vào giỏ mượn!");
            return ResponseEntity.status(401).body(resp);
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        Map<String, Object> result = borrowCartService.addToCart(session, bookId, user);
        return ResponseEntity.ok(result);
    }

    // Xóa khỏi giỏ
    @PostMapping("/remove")
    public String removeFromCart(
            @RequestParam("bookId") Long bookId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        borrowCartService.removeFromCart(session, bookId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài liệu khỏi giỏ mượn.");
        return "redirect:/borrow/cart";
    }

    // Xóa sạch giỏ
    @PostMapping("/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
        borrowCartService.clearCart(session);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa toàn bộ giỏ mượn.");
        return "redirect:/borrow/cart";
    }

    // Xác nhận mượn tất cả sách trong giỏ
    @PostMapping("/checkout")
    public String checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        try {
            List<BorrowRecord> records = borrowCartService.checkout(session, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi thành công yêu cầu mượn cho " + records.size() + " cuốn sách! Sách sẽ được giữ riêng cho bạn trong vòng 30 phút. Vui lòng mang sách tới quầy để thủ thư duyệt mượn.");
            return "redirect:/borrow/history";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/borrow/cart";
        }
    }

    // AJAX lấy số lượng giỏ
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCartCount(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpSession session) {

        Map<String, Object> resp = new HashMap<>();
        resp.put("count", borrowCartService.getCartCount(session));
        if (userDetails != null) {
            User user = userService.getUserByUsername(userDetails.getUsername());
            resp.put("availableQuota", borrowCartService.getAvailableQuota(user));
            resp.put("activeCount", borrowCartService.getActiveBorrowCount(user));
        }
        return ResponseEntity.ok(resp);
    }
}
