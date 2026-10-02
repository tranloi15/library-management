package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookFileService {

    private final BookRepository bookRepository;

    // Tên cột chuẩn (đã chuẩn hóa: chữ thường, bỏ khoảng trắng thừa)
    private static final String COL_TITLE = "tên sách";
    private static final String COL_AUTHOR = "tác giả";
    private static final String COL_ISBN = "isbn";
    private static final String COL_PUBLISHER = "nhà xuất bản";
    private static final String COL_YEAR = "năm xb";
    private static final String COL_QUANTITY = "số lượng";
    private static final String COL_PAGES = "số trang";
    private static final String COL_GENRE = "thể loại";

    // Một vài tên gọi khác của cùng một cột, để file CSV viết hơi khác vẫn nhận được
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("tên sách", COL_TITLE),
            Map.entry("tiêu đề", COL_TITLE),
            Map.entry("title", COL_TITLE),
            Map.entry("tác giả", COL_AUTHOR),
            Map.entry("author", COL_AUTHOR),
            Map.entry("isbn", COL_ISBN),
            Map.entry("nhà xuất bản", COL_PUBLISHER),
            Map.entry("nxb", COL_PUBLISHER),
            Map.entry("publisher", COL_PUBLISHER),
            Map.entry("năm xb", COL_YEAR),
            Map.entry("năm xuất bản", COL_YEAR),
            Map.entry("publish year", COL_YEAR),
            Map.entry("số lượng", COL_QUANTITY),
            Map.entry("quantity", COL_QUANTITY),
            Map.entry("số trang", COL_PAGES),
            Map.entry("page count", COL_PAGES),
            Map.entry("thể loại", COL_GENRE),
            Map.entry("genre", COL_GENRE)
    );

    // ======================= EXPORT =======================

    public byte[] exportBooks() throws IOException {

        List<Book> books = bookRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Danh sách sách");

            String[] headers = {"ID", "Tên sách", "Tác giả", "ISBN", "Nhà xuất bản",
                    "Năm XB", "Số lượng", "Số trang", "Thể loại"};

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (Book book : books) {
                Row row = sheet.createRow(rowIndex++);
                setNumber(row.createCell(0), book.getId());
                row.createCell(1).setCellValue(nullToEmpty(book.getTitle()));
                row.createCell(2).setCellValue(nullToEmpty(book.getAuthor()));
                row.createCell(3).setCellValue(nullToEmpty(book.getIsbn()));
                row.createCell(4).setCellValue(nullToEmpty(book.getPublisher()));
                setNumber(row.createCell(5), book.getPublishYear());
                setNumber(row.createCell(6), book.getQuantity());
                setNumber(row.createCell(7), book.getPageCount());
                row.createCell(8).setCellValue(
                        book.getGenre() != null ? String.valueOf(book.getGenre()) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void setNumber(Cell cell, Number value) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    // ======================= IMPORT =======================

    @Transactional
    public int importBooks(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file CSV.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file CSV.");
        }

        List<Book> books = new ArrayList<>();
        Set<String> isbnInFile = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            Map<String, Integer> columnIndex = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Bỏ ký tự BOM (Excel hay thêm vào đầu file UTF-8)
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = parseCsvLine(line);

                // Dòng không rỗng đầu tiên là dòng tiêu đề -> xác định vị trí từng cột
                if (columnIndex == null) {
                    columnIndex = buildColumnIndex(columns);
                    continue;
                }

                String title = get(columns, columnIndex, COL_TITLE);
                String author = get(columns, columnIndex, COL_AUTHOR);
                String isbn = get(columns, columnIndex, COL_ISBN);
                String publisher = get(columns, columnIndex, COL_PUBLISHER);
                String yearText = get(columns, columnIndex, COL_YEAR);
                String quantityText = get(columns, columnIndex, COL_QUANTITY);
                String pagesText = get(columns, columnIndex, COL_PAGES);
                String genre = get(columns, columnIndex, COL_GENRE);

                if (title.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Dòng " + lineNumber + ": Tên sách không được để trống.");
                }
                if (isbn.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Dòng " + lineNumber + ": ISBN không được để trống.");
                }
                if (!isbnInFile.add(isbn)) {
                    throw new IllegalArgumentException(
                            "Dòng " + lineNumber + ": ISBN bị trùng trong file: " + isbn);
                }
                if (bookRepository.existsByIsbn(isbn)) {
                    throw new IllegalArgumentException(
                            "Dòng " + lineNumber + ": ISBN đã tồn tại trong hệ thống: " + isbn);
                }

                int publishYear = parseInt(yearText, lineNumber, "Năm XB", 0);
                int quantity = parseInt(quantityText, lineNumber, "Số lượng", -1);
                int pageCount = parseInt(pagesText, lineNumber, "Số trang", 0);

                if (quantity <= 0) {
                    throw new IllegalArgumentException(
                            "Dòng " + lineNumber + ": Số lượng phải lớn hơn 0.");
                }
                if (pageCount < 0) {
                    throw new IllegalArgumentException(
                            "Dòng " + lineNumber + ": Số trang không được âm.");
                }

                Book book = new Book(
                        title,
                        publisher.isEmpty() ? null : publisher,
                        publishYear,
                        quantity,
                        null,                               // imageUrl
                        author.isEmpty() ? null : author,
                        isbn,
                        pageCount,
                        genre.isEmpty() ? null : genre
                );

                books.add(book);
            }
        }

        if (books.isEmpty()) {
            throw new IllegalArgumentException("File CSV không có dữ liệu sách hợp lệ.");
        }

        bookRepository.saveAll(books);
        return books.size();
    }

    /** Đọc dòng tiêu đề, trả về map: tên cột chuẩn -> vị trí cột trong file. */
    private Map<String, Integer> buildColumnIndex(String[] headerColumns) {
        Map<String, Integer> index = new HashMap<>();

        for (int i = 0; i < headerColumns.length; i++) {
            String normalized = headerColumns[i].trim().toLowerCase().replaceAll("\\s+", " ");
            String standard = ALIASES.get(normalized);
            if (standard != null && !index.containsKey(standard)) {
                index.put(standard, i);
            }
            // Cột không nhận ra (ví dụ "ID") sẽ bị bỏ qua
        }

        List<String> missing = new ArrayList<>();
        if (!index.containsKey(COL_TITLE)) missing.add("Tên sách");
        if (!index.containsKey(COL_ISBN)) missing.add("ISBN");
        if (!index.containsKey(COL_QUANTITY)) missing.add("Số lượng");

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Dòng tiêu đề của file CSV thiếu cột bắt buộc: " + String.join(", ", missing)
                    + ". Dòng đầu tiên của file phải là tiêu đề cột.");
        }
        return index;
    }

    /** Lấy giá trị của một cột theo tên; trả về "" nếu file không có cột đó hoặc dòng bị thiếu ô. */
    private String get(String[] columns, Map<String, Integer> index, String columnName) {
        Integer i = index.get(columnName);
        if (i == null || i >= columns.length) {
            return "";
        }
        return columns[i].trim();
    }

    /** Parse số nguyên; ô trống thì trả về giá trị mặc định. */
    private int parseInt(String text, int lineNumber, String fieldName, int defaultValue) {
        if (text.isEmpty()) {
            if (defaultValue < 0) {
                throw new IllegalArgumentException(
                        "Dòng " + lineNumber + ": " + fieldName + " không được để trống.");
            }
            return defaultValue;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Dòng " + lineNumber + ": " + fieldName + " không hợp lệ: " + text);
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (insideQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (c == ',' && !insideQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        values.add(current.toString());
        return values.toArray(new String[0]);
    }
}