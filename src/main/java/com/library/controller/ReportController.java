package com.library.controller;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;
import com.library.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        DashboardStatsDto stats = reportService.getDashboardStats();

        model.addAttribute("stats", stats);

        return "home/dashboard";
    }

    @GetMapping("/top-books")
    public String topBooks(
            @RequestParam(defaultValue = "10") int limit,
            Model model) {

        List<TopBookDto> topBooks =
                reportService.getTopBorrowedBooks(limit);

        model.addAttribute("topBooks", topBooks);
        model.addAttribute("limit", limit);

        return "reports/top_books";
    }
}