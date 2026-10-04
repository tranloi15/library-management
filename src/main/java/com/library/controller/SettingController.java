package com.library.controller;

import com.library.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public String viewSettings(Model model) {
        model.addAttribute("settings", settingService.getAllSettingsMap());
        model.addAttribute("activeMenu", "settings");
        return "admin/settings";
    }

    @PostMapping
    public String saveSettings(
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {

        // Xử lý các checkbox toggle: nếu không được tích thì gán giá trị false
        String[] toggleKeys = {
                "feature_online_registration",
                "feature_qr_borrow",
                "feature_self_extension",
                "feature_maintenance_mode"
        };
        for (String key : toggleKeys) {
            if (!allParams.containsKey(key)) {
                allParams.put(key, "false");
            }
        }

        settingService.updateSettings(allParams);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã lưu và cập nhật cấu hình quy định & tính năng hệ thống thành công!"
        );

        return "redirect:/admin/settings";
    }
}
