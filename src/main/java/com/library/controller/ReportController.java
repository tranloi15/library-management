package com.library.controller;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;
import com.library.model.BorrowRecord;
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

    @GetMapping
    public String reports() {
        return "redirect:/reports/top-books";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        DashboardStatsDto stats =
                reportService.getDashboardStats();

        model.addAttribute("activeMenu", "reports");
        model.addAttribute("stats", stats);

        return "home/dashboard";
    }

    @GetMapping("/top-books")
    public String topBooks(
            @RequestParam(defaultValue = "10") int limit,
            Model model) {

        if (limit != 5 && limit != 10) {
            limit = 10;
        }

        List<TopBookDto> topBooks =
                reportService.getTopBorrowedBooks(limit);

        model.addAttribute("activeMenu", "reports");
        model.addAttribute("topBooks", topBooks);
        model.addAttribute("limit", limit);

        return "reports/top_books";
    }

    @GetMapping("/overdue-list")
    public String overdueList(Model model) {

        List<BorrowRecord> overdueRecords =
                reportService.getOverdueRecords();

        long finePerDay = 5000L;

        long totalFine = overdueRecords.stream()
                .mapToLong(record ->
                        record.getOverdueDays() * finePerDay)
                .sum();

        model.addAttribute("activeMenu", "reports");
        model.addAttribute("overdueRecords", overdueRecords);
        model.addAttribute("finePerDay", finePerDay);
        model.addAttribute("totalFine", totalFine);

        return "reports/overdue_list";
    }
}