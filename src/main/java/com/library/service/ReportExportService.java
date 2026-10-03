package com.library.service;

import com.library.dto.TopBookDto;
import com.library.model.BorrowRecord;
import com.library.model.User;
import com.library.service.FineReportService.FineReport;
import com.library.service.FineReportService.FineRow;
import com.library.service.FineReportService.PeriodSummary;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Xuất các báo cáo ra file Excel (.xlsx).
 * Mỗi file gồm: tiêu đề, dòng thông tin, ngày xuất, rồi đến bảng dữ liệu.
 */
@Service
public class ReportExportService {

    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =====================================================================
    // TOP SÁCH MƯỢN NHIỀU NHẤT
    // =====================================================================

    public byte[] exportTopBooks(List<TopBookDto> books, String periodLabel) throws IOException {

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Top sách mượn");
            int rowIndex = writeTitle(wb, sheet,
                    "BÁO CÁO TOP SÁCH MƯỢN NHIỀU NHẤT",
                    "Kỳ báo cáo: " + periodLabel);

            String[] headers = {"Hạng", "Mã tài liệu", "Tên sách", "Tác giả", "ISBN", "Lượt mượn"};
            int headerRow = rowIndex;
            writeHeader(wb, sheet, rowIndex++, headers);

            int rank = 1;
            for (TopBookDto b : books) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(rank++);
                row.createCell(1).setCellValue("DOC-" + b.getBookId());
                row.createCell(2).setCellValue(text(b.getTitle()));
                row.createCell(3).setCellValue(text(b.getAuthor()));
                row.createCell(4).setCellValue(text(b.getIsbn()));
                row.createCell(5).setCellValue(b.getBorrowCount() != null ? b.getBorrowCount() : 0);
            }

            if (books.isEmpty()) {
                sheet.createRow(rowIndex).createCell(0).setCellValue("Không có lượt mượn nào trong kỳ này.");
            }

            finish(sheet, headers.length, headerRow);
            wb.write(out);
            return out.toByteArray();
        }
    }

    // =====================================================================
    // PHIẾU MƯỢN QUÁ HẠN
    // =====================================================================

    public byte[] exportOverdue(List<BorrowRecord> records, long finePerDay) throws IOException {

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Phiếu quá hạn");
            int rowIndex = writeTitle(wb, sheet,
                    "DANH SÁCH PHIẾU MƯỢN QUÁ HẠN",
                    "Mức phạt: " + money(finePerDay) + " đ/ngày • Số phiếu: " + records.size());

            String[] headers = {"Mã phiếu", "Mã độc giả", "Độc giả", "Số điện thoại", "Email",
                    "Tài liệu", "Ngày mượn", "Hạn trả", "Số ngày quá hạn", "Tiền phạt (đ)"};
            int headerRow = rowIndex;
            writeHeader(wb, sheet, rowIndex++, headers);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            long totalFine = 0;

            for (BorrowRecord r : records) {
                User user = r.getUser();
                long fine = r.calculateLateFine(finePerDay);
                totalFine += fine;

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("BR-" + r.getId());
                row.createCell(1).setCellValue(user != null ? user.getReaderCode() : "");
                row.createCell(2).setCellValue(user != null ? text(user.getFullName()) : "Không xác định");
                row.createCell(3).setCellValue(user != null ? text(user.getPhone()) : "");
                row.createCell(4).setCellValue(user != null ? text(user.getEmail()) : "");
                row.createCell(5).setCellValue(r.getDocument() != null ? text(r.getDocument().getTitle()) : "");
                row.createCell(6).setCellValue(date(r.getBorrowDate()));
                row.createCell(7).setCellValue(date(r.getDueDate()));
                row.createCell(8).setCellValue(r.getOverdueDays());

                Cell fineCell = row.createCell(9);
                fineCell.setCellValue(fine);
                fineCell.setCellStyle(moneyStyle);
            }

            if (records.isEmpty()) {
                sheet.createRow(rowIndex++).createCell(0).setCellValue("Không có phiếu mượn quá hạn.");
            }

            // Dòng tổng tiền phạt
            Cell total = sheet.createRow(rowIndex + 1).createCell(0);
            total.setCellValue("Tổng tiền phạt tạm tính: " + money(totalFine) + " đ");
            total.setCellStyle(boldStyle(wb));

            finish(sheet, headers.length, headerRow);
            wb.write(out);
            return out.toByteArray();
        }
    }

    // =====================================================================
    // TIỀN PHẠT ĐÃ THU (sheet 1: chi tiết, sheet 2: so sánh các kỳ)
    // =====================================================================

    public byte[] exportFines(FineReport report, String periodLabel, String comparisonText,
                              List<PeriodSummary> history) throws IOException {

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            // ---------- Sheet 1: chi tiết các khoản phạt ----------
            Sheet sheet = wb.createSheet("Chi tiết");
            String summary = "Tổng đã thu: " + money(report.getTotal()) + " đ";
            if (comparisonText != null) {
                summary += " (" + comparisonText + ")";
            }
            int rowIndex = writeTitle(wb, sheet,
                    "BÁO CÁO TIỀN PHẠT TRẢ SÁCH TRỄ HẠN",
                    "Kỳ báo cáo: " + periodLabel + " (tính theo ngày trả sách) • " + summary);

            String[] headers = {"Mã phiếu", "Ngày trả", "Mã độc giả", "Độc giả", "Số điện thoại",
                    "Tài liệu", "Hạn trả", "Số ngày trễ", "Tiền phạt (đ)", "Phương thức", "Ghi chú"};
            int headerRow = rowIndex;
            writeHeader(wb, sheet, rowIndex++, headers);

            for (FineRow f : report.getRows()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("BR-" + f.getRecordId());
                row.createCell(1).setCellValue(date(f.getReturnDate()));
                row.createCell(2).setCellValue(text(f.getReaderCode()));
                row.createCell(3).setCellValue(text(f.getFullName()));
                row.createCell(4).setCellValue(text(f.getPhone()));
                row.createCell(5).setCellValue(text(f.getDocumentTitle()));
                row.createCell(6).setCellValue(date(f.getDueDate()));
                row.createCell(7).setCellValue(f.getLateDays());
                Cell fineCell = row.createCell(8);
                fineCell.setCellValue(f.getFine());
                fineCell.setCellStyle(moneyStyle);
                row.createCell(9).setCellValue(f.getPaymentLabel());
                row.createCell(10).setCellValue(text(f.getNote()));
            }

            if (report.getRows().isEmpty()) {
                sheet.createRow(rowIndex++).createCell(0).setCellValue("Không có khoản phạt nào trong kỳ này.");
            }

            // Dòng tổng theo phương thức
            CellStyle bold = boldStyle(wb);
            rowIndex++;
            sheet.createRow(rowIndex++).createCell(0).setCellValue(
                    "Tiền mặt: " + money(report.getCashTotal()) + " đ (" + report.getCashCount() + " phiếu)");
            sheet.createRow(rowIndex++).createCell(0).setCellValue(
                    "VietQR: " + money(report.getQrTotal()) + " đ (" + report.getQrCount() + " phiếu)");
            if (report.getOtherCount() > 0) {
                sheet.createRow(rowIndex++).createCell(0).setCellValue(
                        "Chưa ghi nhận phương thức: " + money(report.getOtherTotal()) + " đ ("
                                + report.getOtherCount() + " phiếu)");
            }
            Cell totalCell = sheet.createRow(rowIndex).createCell(0);
            totalCell.setCellValue("TỔNG CỘNG: " + money(report.getTotal()) + " đ");
            totalCell.setCellStyle(bold);

            finish(sheet, headers.length, headerRow);

            // ---------- Sheet 2: so sánh các kỳ ----------
            if (!history.isEmpty()) {
                Sheet compare = wb.createSheet("So sánh các kỳ");
                int r = writeTitle(wb, compare,
                        "SO SÁNH TIỀN PHẠT CÁC KỲ GẦN NHẤT",
                        "Mỗi kỳ được so với kỳ liền trước nó");

                String[] cols = {"Kỳ", "Số khoản phạt", "Tiền mặt (đ)", "VietQR (đ)", "Tổng (đ)",
                        "Thay đổi so với kỳ trước"};
                int compareHeader = r;
                writeHeader(wb, compare, r++, cols);

                for (PeriodSummary p : history) {
                    Row row = compare.createRow(r++);
                    row.createCell(0).setCellValue(p.getName());
                    row.createCell(1).setCellValue(p.getReport().getRows().size());
                    long[] amounts = {p.getReport().getCashTotal(), p.getReport().getQrTotal(), p.getTotal()};
                    for (int i = 0; i < amounts.length; i++) {
                        Cell c = row.createCell(2 + i);
                        c.setCellValue(amounts[i]);
                        c.setCellStyle(moneyStyle);
                    }
                    row.createCell(5).setCellValue(p.getChangeText());
                }

                finish(compare, cols.length, compareHeader);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // =====================================================================
    // HÀM DÙNG CHUNG
    // =====================================================================

    /** Ghi tiêu đề, dòng thông tin và ngày xuất. Trả về vị trí dòng tiếp theo cho bảng. */
    private int writeTitle(Workbook wb, Sheet sheet, String title, String info) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(font);

        Cell titleCell = sheet.createRow(0).createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);

        sheet.createRow(1).createCell(0).setCellValue(info);
        sheet.createRow(2).createCell(0).setCellValue("Ngày xuất: " + LocalDate.now().format(VN_DATE));

        return 4; // dòng 3 để trống
    }

    /** Dòng tiêu đề cột: in đậm, nền xám. */
    private void writeHeader(Workbook wb, Sheet sheet, int rowIndex, String[] headers) {
        CellStyle style = boldStyle(wb);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    /** Căn độ rộng cột và cố định dòng tiêu đề khi cuộn. */
    private void finish(Sheet sheet, int columnCount, int headerRow) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.createFreezePane(0, headerRow + 1);
    }

    private CellStyle boldStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        return style;
    }

    private String text(String s) {
        return s != null ? s : "";
    }

    private String date(LocalDate d) {
        return d != null ? d.format(VN_DATE) : "";
    }

    /** 15000 -> "15.000" */
    private String money(long amount) {
        return String.format("%,d", amount).replace(',', '.');
    }
}