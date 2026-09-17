package com.library.controller;

import com.library.model.User;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public String listReaders(Model model, @RequestParam(value = "keyword", required = false) String keyword) {
        model.addAttribute("readers", userService.searchReaders(keyword));
        model.addAttribute("keyword", keyword);
        return "users/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "users/form";
    }

    @PostMapping("/new")
    public String createReader(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        try {
            userService.createReader(user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thêm độc giả '" + user.getFullName() + "' thành công! Tài khoản: " + user.getEmail() + " | Mật khẩu mặc định: 123456");
            return "redirect:/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("isEdit", true);
        return "users/form";
    }

    @PostMapping("/{id}/edit")
    public String updateReader(@PathVariable("id") Long id,
                               @ModelAttribute("user") User user,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin độc giả thành công!");
            return "redirect:/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/users/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActiveStatus(id);
            User user = userService.getUserById(id);
            String statusText = user.isActive() ? "Mở khóa" : "Khóa";
            redirectAttributes.addFlashAttribute("successMessage", statusText + " tài khoản '" + user.getFullName() + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }
}
