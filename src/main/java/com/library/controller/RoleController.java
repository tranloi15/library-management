package com.library.controller;

import com.library.model.ActionType;
import com.library.model.RoleName;
import com.library.model.TargetType;
import com.library.model.User;
import com.library.service.ActivityLogService;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final UserService userService;
    private final ActivityLogService activityLogService;

    @GetMapping
    public String listAllUsers(Model model,
                               @RequestParam(value = "keyword", required = false) String keyword) {
        model.addAttribute("allUsers", userService.searchAllUsers(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("activeMenu", "roles");
        return "roles/list";
    }

    @PostMapping("/{id}/update")
    public String updateRole(@PathVariable("id") Long id,
                             @RequestParam("role") String roleName,
                             RedirectAttributes redirectAttributes) {
        try {
            RoleName newRole = RoleName.valueOf(roleName);
            userService.updateRole(id, newRole);
            User user = userService.getUserById(id);
            activityLogService.log(
                    ActionType.ROLE_CHANGE,
                    TargetType.USER,
                    user.getId(),
                    user.getFullName() + " (" + user.getUsername() + ")",
                    "Cập nhật quyền hạn của '" + user.getFullName() + "' (" + user.getUsername() + ") thành " + getRoleLabel(newRole)
            );
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã cập nhật quyền của '" + user.getFullName() + "' thành " + getRoleLabel(newRole) + "!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/roles";
    }

    private String getRoleLabel(RoleName role) {
        return switch (role) {
            case ROLE_ADMIN -> "Quản trị viên";
            case ROLE_LIBRARIAN -> "Thủ thư";
            case ROLE_READER -> "Độc giả";
        };
    }
}
