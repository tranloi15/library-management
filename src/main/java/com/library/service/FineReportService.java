package com.library.service;

import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Báo cáo tiền phạt trả sách trễ hạn đã thu, tính theo NGÀY TRẢ sách.
 */
@Service
@RequiredArgsConstructor
public class FineReportService {

    private final BorrowRecordRepository borrowRecordRepository;

    /**
     * Các khoản phạt có ngày trả trong khoảng [from, to].
     * from = null nghĩa là toàn bộ thời gian.
     */
    @Transactional(readOnly = true)
    public FineReport getReport(LocalDate from, LocalDate to) {

        List<BorrowRecord> returned = (from == null)
                ? borrowRecordRepository.findByReturnDateIsNotNull()
                : borrowRecordRepository.findByReturnDateBetween(from, to);

        List<FineRow> rows = returned.stream()
                .filter(r -> r.getFineAmount() != null && r.getFineAmount() > 0)
                .sorted(Comparator.comparing(BorrowRecord::getReturnDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))   // mới nhất trước
                .map(FineRow::new)
                .toList();

        return new FineReport(rows);
    }

    /** Một kỳ báo cáo: tên hiển thị và khoảng ngày. */
    public record Period(String name, LocalDate from, LocalDate to) {
    }

    /**
     * So sánh nhiều kỳ liên tiếp. Danh sách truyền vào sắp từ kỳ mới nhất đến cũ nhất
     * (kỳ đang xem, kỳ trước, kỳ trước nữa...). Mỗi kỳ được so với kỳ liền trước nó.
     */
    @Transactional(readOnly = true)
    public List<PeriodSummary> compareLastPeriods(List<Period> periods) {

        List<FineReport> reports = periods.stream()
                .map(p -> getReport(p.from(), p.to()))
                .toList();

        List<PeriodSummary> result = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            Long previousTotal = (i + 1 < reports.size()) ? reports.get(i + 1).getTotal() : null;
            result.add(new PeriodSummary(periods.get(i).name(), reports.get(i), previousTotal));
        }
        return result;
    }

    // Kết quả báo cáo tổng hợp
    @Getter
    public static class FineReport {

        private final List<FineRow> rows;
        private final long total;
        private final long cashTotal;
        private final long cashCount;
        private final long qrTotal;
        private final long qrCount;

        /** Có tiền phạt nhưng chưa ghi nhận phương thức (cần kiểm tra khi đối soát) */
        private final long otherTotal;
        private final long otherCount;

        public FineReport(List<FineRow> rows) {
            this.rows = rows;
            this.total = sum(rows, null);
            this.cashTotal = sum(rows, "CASH");
            this.cashCount = count(rows, "CASH");
            this.qrTotal = sum(rows, "QR_CODE");
            this.qrCount = count(rows, "QR_CODE");
            this.otherTotal = total - cashTotal - qrTotal;
            this.otherCount = rows.size() - cashCount - qrCount;
        }

        private static long sum(List<FineRow> rows, String method) {
            return rows.stream()
                    .filter(r -> method == null || method.equals(r.getPaymentMethod()))
                    .mapToLong(FineRow::getFine)
                    .sum();
        }

        private static long count(List<FineRow> rows, String method) {
            return rows.stream().filter(r -> method.equals(r.getPaymentMethod())).count();
        }
    }

    /** Tiền phạt của một kỳ, kèm mức thay đổi so với kỳ liền trước. */
    @Getter
    public static class PeriodSummary {

        private final String name;
        private final FineReport report;

        /** Tổng kỳ liền trước, null nếu không có kỳ để so sánh */
        private final Long previousTotal;

        public PeriodSummary(String name, FineReport report, Long previousTotal) {
            this.name = name;
            this.report = report;
            this.previousTotal = previousTotal;
        }

        public long getTotal() {
            return report.getTotal();
        }

        /** UP, DOWN hoặc SAME, dùng để tô màu. */
        public String getChangeDirection() {
            if (previousTotal == null || getTotal() == previousTotal) {
                return "SAME";
            }
            return getTotal() > previousTotal ? "UP" : "DOWN";
        }

        /** Ví dụ: "+25%", "-10%", "Không đổi", "Kỳ trước chưa có". */
        public String getChangeText() {
            if (previousTotal == null) {
                return "—";
            }
            if (previousTotal == 0) {
                return getTotal() > 0 ? "Kỳ trước chưa có" : "Không đổi";
            }
            if (getTotal() == previousTotal) {
                return "Không đổi";
            }
            double percent = (getTotal() - previousTotal) * 100.0 / previousTotal;
            return String.format("%+.0f%%", percent);
        }
    }

    /** Một khoản phạt (thông tin độc giả được lấy sẵn để giao diện và file Excel dùng). */
    @Getter
    public static class FineRow {

        private final Long recordId;
        private final LocalDate returnDate;
        private final LocalDate dueDate;
        private final long lateDays;
        private final long fine;
        private final String paymentMethod;
        private final String paymentLabel;
        private final String note;

        private final String readerCode;
        private final String fullName;
        private final String phone;
        private final String email;
        private final String documentTitle;

        public FineRow(BorrowRecord r) {
            this.recordId = r.getId();
            this.returnDate = r.getReturnDate();
            this.dueDate = r.getDueDate();
            this.lateDays = (r.getDueDate() != null && r.getReturnDate() != null)
                    ? Math.max(0, ChronoUnit.DAYS.between(r.getDueDate(), r.getReturnDate()))
                    : 0;
            this.fine = r.getFineAmount() != null ? r.getFineAmount() : 0;
            this.paymentMethod = r.getPaymentMethod();
            this.paymentLabel = switch (r.getPaymentMethod() == null ? "" : r.getPaymentMethod()) {
                case "CASH" -> "Tiền mặt";
                case "QR_CODE" -> "VietQR";
                default -> "Chưa ghi nhận";
            };
            this.note = r.getNote();

            User user = r.getUser();
            this.readerCode = user != null ? user.getReaderCode() : null;
            this.fullName = user != null ? user.getFullName() : "Không xác định";
            this.phone = user != null ? user.getPhone() : null;
            this.email = user != null ? user.getEmail() : null;
            this.documentTitle = r.getDocument() != null ? r.getDocument().getTitle() : "Không xác định";
        }
    }
}