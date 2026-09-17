package com.library.controller;

import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.service.BorrowService;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;
    private final UserService userService;

    @GetMapping("/history")
    public String viewBorrowHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(userDetails.getUsername());
        List<BorrowRecord> activeBorrows = borrowService.getActiveBorrowsByUser(user.getId());
        List<BorrowRecord> returnedBorrows = borrowService.getReturnedBorrowsByUser(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("returnedBorrows", returnedBorrows);

        return "borrow/history";
    }
}
