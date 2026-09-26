package com.library.controller;

import com.library.model.BorrowRecord;
import com.library.model.Document;
import com.library.model.DocumentType;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Document> allDocs = documentRepository.findAll();
        List<User> allUsers = userRepository.findAll();
        List<BorrowRecord> allRecords = borrowRecordRepository.findAll();

        long totalDocuments = allDocs.size();
        long totalBooks = allDocs.stream().filter(d -> d.getDocumentType() == DocumentType.BOOK).count();
        long totalMagazines = allDocs.stream().filter(d -> d.getDocumentType() == DocumentType.MAGAZINE).count();

        long totalPatrons = allUsers.stream()
                .filter(u -> u.getRole() != null && u.getRole().name().equals("ROLE_READER")).count();
        long activePatrons = allUsers.stream()
                .filter(u -> u.getRole() != null && u.getRole().name().equals("ROLE_READER") && u.isActive()).count();

        long activeBorrows = allRecords.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() != com.library.model.BorrowStatus.RETURNED)
                .count();
        long overdueBorrows = allRecords.stream().filter(BorrowRecord::isOverdue).count();

        List<BorrowRecord> recentRecords = allRecords.stream()
                .sorted(Comparator.comparing(BorrowRecord::getBorrowDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .toList();

        List<BorrowRecord> overdueRecords = allRecords.stream()
                .filter(BorrowRecord::isOverdue)
                .sorted(Comparator.comparing(BorrowRecord::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(4)
                .toList();

        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("totalDocuments", totalDocuments);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalMagazines", totalMagazines);
        model.addAttribute("totalPatrons", totalPatrons > 0 ? totalPatrons : allUsers.size());
        model.addAttribute("activePatrons", activePatrons > 0 ? activePatrons : allUsers.size());
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("overdueBorrows", overdueBorrows);
        model.addAttribute("recentRecords", recentRecords);
        model.addAttribute("overdueRecords", overdueRecords);
        model.addAttribute("today", LocalDate.now());

        return "dashboard";
    }
}
