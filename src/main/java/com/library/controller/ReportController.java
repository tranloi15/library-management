package com.library.controller;

import com.library.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

private final ReportService reportService;

@GetMapping("/dashboard")
public String dashboard(Model model) {

    model.addAttribute(
            "stats",
            reportService.getDashboardStats()
    );

    return "home/dashboard";
}

@GetMapping("/top-books")
public String topBooks(
        @RequestParam(defaultValue = "5") int limit,
        Model model
) {

    if (limit != 5 && limit != 10) {
        limit = 5;
    }

    model.addAttribute(
            "topBooks",
            reportService.getTopBorrowedBooks(limit)
    );

    model.addAttribute("limit", limit);

    return "reports/top_books";
}

@GetMapping("/overdue")
public String overdueBooks(Model model) {

    model.addAttribute(
            "overdueRecords",
            reportService.getOverdueRecords()
    );

    return "reports/overdue_list";
}

}
