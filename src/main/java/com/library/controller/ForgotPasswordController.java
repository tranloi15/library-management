package com.library.controller;

import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;

    // ===== TRANG QUÊN MẬT KHẨU =====
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        String token = userService.generatePasswordResetToken(email);
        if (token != null) {
            // Trong thực tế: gửi email chứa link reset
            // Ở đây: redirect thẳng đến trang reset với token (demo mode)
            redirectAttributes.addFlashAttribute("resetToken", token);
            redirectAttributes.addFlashAttribute("successMessage",
                "Yêu cầu hợp lệ! Trong hệ thống thực, một email sẽ được gửi đến '" + email + "'. " +
                "Đây là demo: token của bạn đã được tạo.");
            return "redirect:/reset-password?token=" + token;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Không tìm thấy tài khoản với email '" + email + "'. Vui lòng kiểm tra lại!");
            return "redirect:/forgot-password";
        }
    }

    // ===== TRANG ĐẶT LẠI MẬT KHẨU =====
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "token", required = false) String token,
                                        Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("errorMessage", "Token không hợp lệ hoặc đã hết hạn!");
            return "auth/reset-password";
        }
        if (!userService.isValidResetToken(token)) {
            model.addAttribute("errorMessage", "Token không hợp lệ hoặc đã hết hạn (1 giờ)! Vui lòng yêu cầu lại.");
            return "auth/reset-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "redirect:/reset-password?token=" + token;
        }
        try {
            userService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                "Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
}
