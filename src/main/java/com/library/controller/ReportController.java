package com.library.controller;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;
import com.library.model.BorrowRecord;
import com.library.service.FineReportService;
import com.library.service.FineReportService.FineReport;
import com.library.service.FineReportService.PeriodSummary;
import com.library.service.ReportExportService;
import com.library.service.ReportService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Mức phạt trả trễ mỗi ngày (đồng). */
    private static final long FINE_PER_DAY = 5000L;

    /** Số kỳ hiển thị trong bảng so sánh (kỳ đang xem + 3 kỳ trước), dùng cho Top sách và Tiền phạt. */
    private static final int COMPARE_PERIODS = 4;

    private final ReportService reportService;
    private final ReportExportService reportExportService;
    private final FineReportService fineReportService;

    @GetMapping
    public String reports() {
        return "redirect:/reports/top-books";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        DashboardStatsDto stats =
                reportService.getDashboardStats();

        model.addAttribute("stats", stats);

        return "home/dashboard";
    }

    // =====================================================================
    // TOP SÁCH MƯỢN
    // =====================================================================

    /**
     * Top sách mượn nhiều nhất, có lọc theo thời gian.
     * period: ALL (mặc định) | MONTH | QUARTER | YEAR | CUSTOM
     * Ví dụ: /reports/top-books?period=QUARTER&quarter=3&year=2026&limit=10
     */
    @GetMapping("/top-books")
    public String topBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        limit = normalizeLimit(limit);
        PeriodRange range = resolvePeriod(period, quarter, year, from, to);

        model.addAttribute("topBooks", loadTopBooks(range, limit));
        model.addAttribute("limit", limit);
        addPeriodAttributes(model, range);

        if (range.from() == null) {
            // Toàn bộ thời gian: không có kỳ trước để so sánh
            model.addAttribute("totalBorrows", null);
            model.addAttribute("borrowHistory", List.of());
            model.addAttribute("previousRanks", null);
            model.addAttribute("borrowComparisonText", null);
        } else {
            List<BorrowPeriodSummary> history = loadBorrowHistory(range);
            model.addAttribute("totalBorrows", history.get(0).getTotal());
            model.addAttribute("borrowHistory", history);
            model.addAttribute("previousRanks", previousRanks(range));
            model.addAttribute("borrowComparisonText", borrowComparisonText(history, range));
        }

        return "reports/top_books";
    }

    /** Tổng lượt mượn và sách đứng đầu của kỳ đang xem và các kỳ liền trước (mới nhất trước). */
    private List<BorrowPeriodSummary> loadBorrowHistory(PeriodRange range) {

        List<PeriodRange> periods = new ArrayList<>();
        PeriodRange r = range;
        while (r != null && periods.size() < COMPARE_PERIODS) {
            periods.add(r);
            r = previousPeriod(r);
        }

        List<Long> totals = periods.stream()
                .map(p -> reportService.countBorrowsBetween(p.from(), p.to()))
                .toList();

        List<BorrowPeriodSummary> history = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            PeriodRange p = periods.get(i);
            List<TopBookDto> top = reportService.getTopBorrowedBooks(p.from(), p.to(), 1);
            Long previousTotal = (i + 1 < totals.size()) ? totals.get(i + 1) : null;
            history.add(new BorrowPeriodSummary(
                    p.name(),
                    totals.get(i),
                    top.isEmpty() ? null : top.get(0).getTitle(),
                    top.isEmpty() ? 0 : top.get(0).getBorrowCount(),
                    previousTotal));
        }
        return history;
    }

    /** Thứ hạng của từng sách ở kỳ trước: bookId -> hạng (1 = cao nhất). */
    private Map<Long, Integer> previousRanks(PeriodRange range) {
        PeriodRange prev = previousPeriod(range);
        Map<Long, Integer> ranks = new HashMap<>();
        if (prev == null) {
            return ranks;
        }
        List<TopBookDto> ranking = reportService.getTopBorrowedBooks(prev.from(), prev.to(), Integer.MAX_VALUE);
        for (int i = 0; i < ranking.size(); i++) {
            ranks.put(ranking.get(i).getBookId(), i + 1);
        }
        return ranks;
    }

    /** Ví dụ: "+43% so với quý trước (7 lượt)". */
    private String borrowComparisonText(List<BorrowPeriodSummary> history, PeriodRange range) {
        if (history.size() < 2) {
            return null;
        }
        long previousTotal = history.get(1).getTotal();
        if (previousTotal == 0) {
            return previousName(range) + " chưa có lượt mượn nào";
        }
        return history.get(0).getChangeText() + " so với " + previousName(range)
                + " (" + previousTotal + " lượt)";
    }

    /** Lượt mượn của một kỳ, kèm mức thay đổi so với kỳ liền trước. */
    @Getter
    public static class BorrowPeriodSummary {

        private final String name;
        private final long total;
        private final String topTitle;
        private final long topCount;
        private final Long previousTotal;

        public BorrowPeriodSummary(String name, long total, String topTitle, long topCount, Long previousTotal) {
            this.name = name;
            this.total = total;
            this.topTitle = topTitle;
            this.topCount = topCount;
            this.previousTotal = previousTotal;
        }

        /** UP, DOWN hoặc SAME, dùng để tô màu. */
        public String getChangeDirection() {
            if (previousTotal == null || total == previousTotal) {
                return "SAME";
            }
            return total > previousTotal ? "UP" : "DOWN";
        }

        /** Ví dụ: "+43%", "-10%", "Không đổi", "Kỳ trước chưa có". */
        public String getChangeText() {
            if (previousTotal == null) {
                return "—";
            }
            if (previousTotal == 0) {
                return total > 0 ? "Kỳ trước chưa có" : "Không đổi";
            }
            if (total == previousTotal) {
                return "Không đổi";
            }
            return String.format("%+.0f%%", (total - previousTotal) * 100.0 / previousTotal);
        }
    }

    /** Xuất bảng Top sách của đúng kỳ đang xem ra file Excel. */
    @GetMapping("/top-books/export")
    public ResponseEntity<byte[]> exportTopBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {

        limit = normalizeLimit(limit);
        PeriodRange range = resolvePeriod(period, quarter, year, from, to);

        byte[] file = reportExportService.exportTopBooks(loadTopBooks(range, limit), range.label());

        return excelResponse(file, "top-sach_" + fileSuffix(range) + ".xlsx");
    }

    private List<TopBookDto> loadTopBooks(PeriodRange range, int limit) {
        if (range.from() == null) {
            return reportService.getTopBorrowedBooks(limit);
        }
        return reportService.getTopBorrowedBooks(range.from(), range.to(), limit);
    }

    // =====================================================================
    // PHIẾU QUÁ HẠN
    // =====================================================================

    @GetMapping("/overdue-list")
    public String overdueList(Model model) {

        List<BorrowRecord> overdueRecords =
                reportService.getOverdueRecords();

        long totalFine = overdueRecords.stream()
                .mapToLong(record ->
                        record.getOverdueDays() * FINE_PER_DAY)
                .sum();

        model.addAttribute("overdueRecords", overdueRecords);
        model.addAttribute("finePerDay", FINE_PER_DAY);
        model.addAttribute("totalFine", totalFine);

        return "reports/overdue_list";
    }

    /** Xuất danh sách phiếu quá hạn (kèm số điện thoại, tiền phạt) để liên hệ thu hồi sách. */
    @GetMapping("/overdue-list/export")
    public ResponseEntity<byte[]> exportOverdueList() throws IOException {

        byte[] file = reportExportService.exportOverdue(reportService.getOverdueRecords(), FINE_PER_DAY);

        return excelResponse(file, "phieu-qua-han_" + LocalDate.now() + ".xlsx");
    }

    // =====================================================================
    // TIỀN PHẠT (tính theo ngày trả sách, có so sánh các kỳ trước)
    // =====================================================================

    @GetMapping("/fines")
    public String fines(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        PeriodRange range = resolvePeriod(period, quarter, year, from, to);
        List<PeriodSummary> history = loadFineHistory(range);
        FineReport report = history.isEmpty()
                ? fineReportService.getReport(null, null)
                : history.get(0).getReport();

        model.addAttribute("report", report);
        model.addAttribute("history", history);
        model.addAttribute("previousName", previousName(range));
        model.addAttribute("comparisonText", comparisonText(history, range));
        addPeriodAttributes(model, range);

        return "reports/fines";
    }

    /** Xuất báo cáo tiền phạt của đúng kỳ đang xem (kèm sheet so sánh các kỳ). */
    @GetMapping("/fines/export")
    public ResponseEntity<byte[]> exportFines(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {

        PeriodRange range = resolvePeriod(period, quarter, year, from, to);
        List<PeriodSummary> history = loadFineHistory(range);
        FineReport report = history.isEmpty()
                ? fineReportService.getReport(null, null)
                : history.get(0).getReport();

        byte[] file = reportExportService.exportFines(report, range.label(),
                comparisonText(history, range), history);

        return excelResponse(file, "tien-phat_" + fileSuffix(range) + ".xlsx");
    }

    /** Kỳ đang xem và các kỳ liền trước (mới nhất trước). Rỗng nếu xem toàn bộ thời gian. */
    private List<PeriodSummary> loadFineHistory(PeriodRange range) {
        if (range.from() == null) {
            return List.of();
        }
        List<FineReportService.Period> periods = new ArrayList<>();
        PeriodRange r = range;
        while (r != null && periods.size() < COMPARE_PERIODS) {
            periods.add(new FineReportService.Period(r.name(), r.from(), r.to()));
            r = previousPeriod(r);
        }
        return fineReportService.compareLastPeriods(periods);
    }

    /** Ví dụ: "+25% so với quý trước (40.000 đ)". Null nếu không so sánh. */
    private String comparisonText(List<PeriodSummary> history, PeriodRange range) {
        if (history.size() < 2) {
            return null;
        }
        PeriodSummary current = history.get(0);
        long previousTotal = history.get(1).getTotal();
        String change = current.getChangeText();
        if (previousTotal == 0) {
            return previousName(range) + " chưa thu khoản phạt nào";
        }
        return change + " so với " + previousName(range)
                + " (" + String.format("%,d", previousTotal).replace(',', '.') + " đ)";
    }

    // =====================================================================
    // KHOẢNG THỜI GIAN BÁO CÁO
    // =====================================================================

    /**
     * Khoảng thời gian của báo cáo. from/to = null nghĩa là toàn bộ thời gian.
     * name: tên ngắn ("Quý 3/2026"); label: tên đầy đủ kèm ngày.
     */
    private record PeriodRange(String type, int quarter, int year,
                               LocalDate from, LocalDate to, String name, String label) {
    }

    /** Tính khoảng ngày từ tham số lọc trên URL. */
    private PeriodRange resolvePeriod(String period, Integer quarter, Integer year,
                                      LocalDate from, LocalDate to) {

        LocalDate today = LocalDate.now();

        String type = (period == null || period.isBlank()) ? "ALL" : period.trim().toUpperCase();
        int y = (year == null || year < 2000 || year > 2100) ? today.getYear() : year;
        int q = (quarter == null || quarter < 1 || quarter > 4) ? quarterOf(today) : quarter;

        return switch (type) {
            case "MONTH" -> monthRange(YearMonth.from(today));
            case "QUARTER" -> quarterRange(y, q);
            case "YEAR" -> yearRange(y);
            case "CUSTOM" -> customRange(
                    from != null ? from : today.withDayOfMonth(1),
                    to != null ? to : today);
            default -> new PeriodRange("ALL", q, y, null, null,
                    "Toàn bộ thời gian", "Toàn bộ thời gian");
        };
    }

    /** Kỳ liền trước có cùng độ dài. Null nếu đang xem toàn bộ thời gian. */
    private PeriodRange previousPeriod(PeriodRange r) {
        return switch (r.type()) {
            case "MONTH" -> monthRange(YearMonth.from(r.from()).minusMonths(1));
            case "QUARTER" -> r.quarter() == 1 ? quarterRange(r.year() - 1, 4) : quarterRange(r.year(), r.quarter() - 1);
            case "YEAR" -> yearRange(r.year() - 1);
            case "CUSTOM" -> {
                long days = ChronoUnit.DAYS.between(r.from(), r.to()) + 1;
                LocalDate prevTo = r.from().minusDays(1);
                yield customRange(prevTo.minusDays(days - 1), prevTo);
            }
            default -> null;
        };
    }

    private String previousName(PeriodRange r) {
        return switch (r.type()) {
            case "MONTH" -> "tháng trước";
            case "QUARTER" -> "quý trước";
            case "YEAR" -> "năm trước";
            case "CUSTOM" -> "kỳ trước";
            default -> "";
        };
    }

    private PeriodRange monthRange(YearMonth ym) {
        return build("MONTH", ym.atDay(1), ym.atEndOfMonth(),
                "Tháng " + ym.getMonthValue() + "/" + ym.getYear());
    }

    private PeriodRange quarterRange(int year, int quarter) {
        LocalDate start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        return build("QUARTER", start, start.plusMonths(3).minusDays(1), "Quý " + quarter + "/" + year);
    }

    private PeriodRange yearRange(int year) {
        return build("YEAR", LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), "Năm " + year);
    }

    private PeriodRange customRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {           // nhập ngược thì tự đảo lại
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        String dates = start.format(VN_DATE) + " – " + end.format(VN_DATE);
        return new PeriodRange("CUSTOM", quarterOf(start), start.getYear(), start, end,
                dates, "Tùy chọn (" + dates + ")");
    }

    private PeriodRange build(String type, LocalDate start, LocalDate end, String name) {
        String label = name + " (" + start.format(VN_DATE) + " – " + end.format(VN_DATE) + ")";
        return new PeriodRange(type, quarterOf(start), start.getYear(), start, end, name, label);
    }

    private int quarterOf(LocalDate date) {
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    /** Đưa bộ lọc kỳ đang chọn lên giao diện (để giữ lựa chọn và tạo link). */
    private void addPeriodAttributes(Model model, PeriodRange range) {
        model.addAttribute("period", range.type());
        model.addAttribute("quarter", range.quarter());
        model.addAttribute("year", range.year());
        model.addAttribute("fromDate", range.from());
        model.addAttribute("toDate", range.to());
        model.addAttribute("periodLabel", range.label());
    }

    private String fileSuffix(PeriodRange range) {
        return range.from() == null ? "toan-bo" : range.from() + "_" + range.to();
    }

    // =====================================================================
    // HÀM DÙNG CHUNG
    // =====================================================================

    private int normalizeLimit(int limit) {
        return (limit == 5 || limit == 10) ? limit : 10;
    }

    /** Trả file Excel về trình duyệt để tải xuống. */
    private ResponseEntity<byte[]> excelResponse(byte[] file, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}