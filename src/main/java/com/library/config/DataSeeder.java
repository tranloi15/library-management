package com.library.config;

import com.library.model.*;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedUsers();
        }
        if (documentRepository.count() == 0) {
            seedDocuments();
        }
        if (borrowRecordRepository.count() == 0) {
            seedBorrowRecords();
        }
        // Chạy mỗi lần khởi động: gán ảnh bìa cho các sách chưa có ảnh (ví dụ sách nhập từ CSV)
        fillMissingBookCovers();
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode("123456");

        User admin = new User("admin", encodedPassword, "Quản Trị Viên", "admin@library.com", "0988000111", "Hà Nội",
                LocalDate.of(1990, 5, 15), RoleName.ROLE_ADMIN);

        User reader1 = new User("reader1", encodedPassword, "Nguyễn Văn An", "reader1@gmail.com",
                "0912345678", "Cầu Giấy, Hà Nội", LocalDate.of(1998, 8, 20), RoleName.ROLE_READER);

        User reader2 = new User("reader2", encodedPassword, "Trần Thị Bình", "reader2@gmail.com",
                "0923456789", "Hải Châu, Đà Nẵng", LocalDate.of(2001, 12, 10), RoleName.ROLE_READER);

        User reader3 = new User("reader3", encodedPassword, "Lê Hoàng Nam", "reader3@gmail.com",
                "0934567890", "Quận 1, TP. Hồ Chí Minh", LocalDate.of(1995, 3, 25), RoleName.ROLE_READER);

        User reader4 = new User("reader4", encodedPassword, "Phạm Minh Châu", "reader4@gmail.com",
                "0945678901", "Đống Đa, Hà Nội", LocalDate.of(2000, 7, 18), RoleName.ROLE_READER);

        User reader5 = new User("reader5", encodedPassword, "Vũ Hải Đăng", "reader5@gmail.com",
                "0956789012", "Thanh Khê, Đà Nẵng", LocalDate.of(2002, 11, 5), RoleName.ROLE_READER);

        User reader6 = new User("reader6", encodedPassword, "Đỗ Phương Linh", "reader6@gmail.com",
                "0967890123", "Bình Thạnh, TP. Hồ Chí Minh", LocalDate.of(1999, 9, 30), RoleName.ROLE_READER);

        userRepository.saveAll(List.of(admin, reader1, reader2, reader3, reader4, reader5, reader6));
    }

    private void seedDocuments() {
        List<Document> documents = List.of(
                // Sách học thuật & kỹ thuật công nghệ
                new Book("Clean Code", "Prentice Hall", 2008, 6, "/images/clean_code.jpg", "Robert C. Martin",
                        "9780132350884", 464, "Software Engineering",
                        "Tác phẩm kinh điển của Uncle Bob hướng dẫn triết lý, nguyên tắc và kỹ năng viết mã nguồn sạch, dễ đọc, dễ bảo trì và giảm thiểu tối đa nợ kỹ thuật cho lập trình viên."),
                new Book("Effective Java", "Addison-Wesley", 2018, 4, "/images/effective_java.jpg", "Joshua Bloch",
                        "9780134685991", 416, "Programming",
                        "Cẩm nang gối đầu giường của cựu kỹ sư trưởng Java Joshua Bloch, bao gồm 90 nguyên tắc vàng thiết kế lớp, quản lý tài nguyên, generics và lập trình bất đồng bộ tối ưu."),
                new Book("Design Patterns", "Addison-Wesley", 1994, 3, "/images/gof.jpg", "Erich Gamma et al.",
                        "9780201633610", 395, "Software Architecture",
                        "Cuốn sách khai sinh khái niệm Mẫu thiết kế phần mềm, phân tích sâu sắc 23 mẫu kinh điển giúp giải quyết các vấn đề lặp lại trong kiến trúc phần mềm hướng đối tượng."),
                new Book("Refactoring", "Addison-Wesley", 2018, 1, "/images/refactor.jpg", "Martin Fowler",
                        "9780134757599", 448, "Refactoring",
                        "Phương pháp luận chuẩn mực về cải tiến cấu trúc bên trong của mã nguồn mà không thay đổi hành vi bên ngoài, giúp hệ thống luôn linh hoạt trước yêu cầu mới."),
                new Book("Head First Java", "O'Reilly", 2022, 8, "/images/hf_java.jpg", "Kathy Sierra",
                        "9781491910771", 750, "Education",
                        "Phương pháp tiếp cận thị giác trực quan, hài hước và dễ hiểu dành cho người học nhập môn Java, lập trình hướng đối tượng, xử lý đa luồng và mạng máy tính."),
                new Book("Spring Boot in Action", "Manning", 2018, 5, "/images/spring_action.jpg", "Craig Walls",
                        "9781617292545", 264, "Framework",
                        "Hướng dẫn toàn diện cách xây dựng ứng dụng web và vi dịch vụ hiện đại bằng Spring Boot, từ tự động cấu hình, Spring Security đến triển khai cloud."),
                new Book("Dế Mèn Phiêu Lưu Ký", "NXB Kim Đồng", 2020, 12, "/images/demen.jpg", "Tô Hoài",
                        "9786042188888", 188, "Văn học thiếu nhi",
                        "Kiệt tác văn học thiếu nhi Việt Nam kể về hành trình chu du, trưởng thành của chú Dế Mèn và thông điệp cao đẹp về tình hữu ái, hòa bình và lý tưởng sống tích cực."),

                // Tạp chí định kỳ chuyên ngành
                new Magazine("Tạp chí Tin học & Điều khiển", "Viện Hàn Lâm KH&CN", 2026, 8, "/images/cntt.jpg", 1, 3,
                        "Số đặc biệt công bố các công trình nghiên cứu mới về Trí tuệ nhân tạo (AI), Hệ thống nhúng tự động hóa và An toàn không gian mạng."),
                new Magazine("National Geographic Vietnam", "NXB Trẻ", 2026, 6, "/images/natgeo.jpg", 45, 6,
                        "Ấn bản khám phá thế giới tự nhiên, bảo tồn đa dạng sinh học và các nền văn hóa bản địa độc đáo qua lăng kính nhiếp ảnh phóng sự chuyên sâu."),
                new Magazine("Tạp chí Kinh tế Sài Gòn", "Saigon Times", 2026, 5, "/images/kinhte.jpg", 120, 8,
                        "Phân tích chuyên sâu về kinh tế vĩ mô, biến động thị trường tài chính và các chiến lược chuyển đổi số trong khối doanh nghiệp Việt Nam."));

        documentRepository.saveAll(documents);
    }

    private void seedBorrowRecords() {
        List<User> readers = userRepository.findByRole(RoleName.ROLE_READER);
        List<Document> docs = documentRepository.findAll();

        if (readers.size() >= 6 && docs.size() >= 10) {
            LocalDate today = LocalDate.now();

            // Độc giả reader1 (Nguyễn Văn An)
            // Sách đang mượn còn hạn, có thể xin gia hạn trực tuyến
            BorrowRecord b1 = new BorrowRecord(
                    docs.get(0), // Clean Code
                    readers.get(0),
                    today.minusDays(3),
                    today.plusDays(11),
                    BorrowStatus.BORROWING);
            docs.get(0).adjustQuantity(-1);

            // Sách đang mượn đã quá hạn, hệ thống tính phạt tự động
            BorrowRecord b2 = new BorrowRecord(
                    docs.get(4), // Head First Java
                    readers.get(0),
                    today.minusDays(16),
                    today.minusDays(2),
                    BorrowStatus.OVERDUE);
            docs.get(4).adjustQuantity(-1);

            // Sách đã trả đúng thời hạn hôm nay, không phạt
            BorrowRecord b3 = new BorrowRecord(
                    docs.get(1), // Effective Java
                    readers.get(0),
                    today.minusDays(10),
                    today.plusDays(4),
                    today,
                    BorrowStatus.RETURNED);
            b3.setPaymentMethod("NONE");
            b3.setFineAmount(0L);
            b3.setNote("Sách nguyên vẹn, trả đúng thời hạn");

            // Sách đã trả trễ hạn, đã nộp phạt qua mã QR VietQR
            BorrowRecord b4 = new BorrowRecord(
                    docs.get(5), // Spring Boot in Action
                    readers.get(0),
                    today.minusDays(18),
                    today.minusDays(4),
                    today.minusDays(1),
                    BorrowStatus.RETURNED);
            b4.setPaymentMethod("QR_CODE");
            b4.setFineAmount(15000L);
            b4.setNote("Đã thanh toán 15.000đ qua VietQR MB Bank");

            // Độc giả reader2 (Trần Thị Bình)
            // Sách đang mượn quá hạn
            BorrowRecord b5 = new BorrowRecord(
                    docs.get(2), // Design Patterns
                    readers.get(1),
                    today.minusDays(17),
                    today.minusDays(3),
                    BorrowStatus.OVERDUE);
            docs.get(2).adjustQuantity(-1);

            // Sách đã trả đúng hạn hôm nay
            BorrowRecord b6 = new BorrowRecord(
                    docs.get(6), // Dế Mèn Phiêu Lưu Ký
                    readers.get(1),
                    today.minusDays(14),
                    today,
                    today,
                    BorrowStatus.RETURNED);
            b6.setPaymentMethod("NONE");
            b6.setFineAmount(0L);
            b6.setNote("Trả sách nguyên vẹn tại quầy");

            // Độc giả reader3 (Lê Hoàng Nam)
            // Sách mới mượn hôm nay còn hạn tiêu chuẩn
            BorrowRecord b7 = new BorrowRecord(
                    docs.get(3), // Refactoring
                    readers.get(2),
                    today,
                    today.plusDays(14),
                    BorrowStatus.BORROWING);
            docs.get(3).adjustQuantity(-1);

            // Sách đang mượn quá hạn
            BorrowRecord b8 = new BorrowRecord(
                    docs.get(5), // Spring Boot in Action
                    readers.get(2),
                    today.minusDays(19),
                    today.minusDays(5),
                    BorrowStatus.OVERDUE);
            docs.get(5).adjustQuantity(-1);

            // Sách đã trả đúng hạn gần đây
            BorrowRecord b9 = new BorrowRecord(
                    docs.get(0), // Clean Code
                    readers.get(2),
                    today.minusDays(12),
                    today.plusDays(2),
                    today.minusDays(2),
                    BorrowStatus.RETURNED);
            b9.setPaymentMethod("NONE");
            b9.setFineAmount(0L);

            // Độc giả reader4 (Phạm Minh Châu)
            // Tạp chí đang mượn còn hạn
            BorrowRecord b10 = new BorrowRecord(
                    docs.get(7), // Tạp chí Tin học & Điều khiển
                    readers.get(3),
                    today.minusDays(2),
                    today.plusDays(12),
                    BorrowStatus.BORROWING);
            docs.get(7).adjustQuantity(-1);

            // Sách đã trả trễ hạn, đã nộp phạt tiền mặt tại quầy
            BorrowRecord b11 = new BorrowRecord(
                    docs.get(0), // Clean Code
                    readers.get(3),
                    today.minusDays(20),
                    today.minusDays(6),
                    today.minusDays(2),
                    BorrowStatus.RETURNED);
            b11.setPaymentMethod("CASH");
            b11.setFineAmount(20000L);
            b11.setNote("Đã thu 20.000đ tiền mặt tại quầy");

            // Độc giả reader5 (Vũ Hải Đăng)
            // Tạp chí mới mượn hôm qua còn hạn
            BorrowRecord b12 = new BorrowRecord(
                    docs.get(8), // National Geographic
                    readers.get(4),
                    today.minusDays(1),
                    today.plusDays(13),
                    BorrowStatus.BORROWING);
            docs.get(8).adjustQuantity(-1);

            // Cập nhật lại số lượng tồn kho của tài liệu sau khi trừ các sách đang mượn
            documentRepository.saveAll(docs);

            borrowRecordRepository.saveAll(List.of(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12));
        }
    }

    /**
     * Gắn ảnh bìa đúng với từng cuốn sách theo ISBN:
     *  1. Nếu có file static/images/covers/{ISBN}.jpg trong dự án -> dùng ảnh trong máy
     *     (chạy được cả khi không có mạng).
     *  2. Nếu không có file -> dùng link Open Library theo ISBN.
     * Chỉ thay ảnh cho sách chưa có ảnh hoặc đang dùng link Open Library;
     * ảnh của sách mẫu (/images/clean_code.jpg, ...) và tạp chí giữ nguyên.
     */
    private void fillMissingBookCovers() {
        List<Document> changed = new ArrayList<>();

        for (Document d : documentRepository.findAll()) {
            if (!(d instanceof Book book)) {
                continue;
            }
            String isbn = book.getIsbn();
            if (isbn == null || isbn.isBlank()) {
                continue;
            }
            isbn = isbn.trim();

            String current = d.getImageUrl();
            boolean noImage = current == null || current.isBlank();
            boolean onlineCover = !noImage && current.startsWith("https://covers.openlibrary.org");
            if (!noImage && !onlineCover) {
                continue; // đã có ảnh riêng, không đụng tới
            }

            String newUrl = hasLocalCover(isbn) ? "/images/covers/" + isbn + ".jpg" : coverUrlFromIsbn(isbn);

            if (!newUrl.equals(current)) {
                d.setImageUrl(newUrl);
                changed.add(d);
            }
        }

        if (!changed.isEmpty()) {
            documentRepository.saveAll(changed);
        }
    }

    /** Kiểm tra dự án có file ảnh bìa static/images/covers/{ISBN}.jpg hay không. */
    private boolean hasLocalCover(String isbn) {
        return new ClassPathResource("static/images/covers/" + isbn + ".jpg").exists();
    }

    /**
     * Link ảnh bìa trên Open Library theo ISBN.
     * default=false: nếu không có bìa sẽ trả lỗi 404 để giao diện hiện ảnh mặc định.
     */
    private String coverUrlFromIsbn(String isbn) {
        return "https://covers.openlibrary.org/b/isbn/" + isbn.trim() + "-L.jpg?default=false";
    }
}