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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final com.library.repository.ActivityLogRepository activityLogRepository;
    private final com.library.repository.SystemSettingRepository systemSettingRepository;
    private final com.library.repository.NotificationRepository notificationRepository;
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
        if (activityLogRepository.count() == 0) {
            seedActivityLogs();
        }
        if (systemSettingRepository.count() == 0) {
            seedSystemSettings();
        }
        if (notificationRepository.count() == 0) {
            seedNotifications();
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
        Book b1 = new Book("Clean Code", "Prentice Hall", 2008, 6, "/images/clean_code.jpg", "Robert C. Martin",
                "9780132350884", 464, "Software Engineering",
                "Tác phẩm kinh điển của Uncle Bob hướng dẫn triết lý, nguyên tắc và kỹ năng viết mã nguồn sạch, dễ đọc, dễ bảo trì và giảm thiểu tối đa nợ kỹ thuật cho lập trình viên.");
        b1.setShelfLocation("Kệ A1 - Tầng 2");

        Book b2 = new Book("Effective Java", "Addison-Wesley", 2018, 4, "/images/effective_java.jpg", "Joshua Bloch",
                "9780134685991", 416, "Programming",
                "Cẩm nang gối đầu giường của cựu kỹ sư trưởng Java Joshua Bloch, bao gồm 90 nguyên tắc vàng thiết kế lớp, quản lý tài nguyên, generics và lập trình bất đồng bộ tối ưu.");
        b2.setShelfLocation("Kệ A2 - Tầng 2");

        Book b3 = new Book("Design Patterns", "Addison-Wesley", 1994, 3, "/images/gof.jpg", "Erich Gamma et al.",
                "9780201633610", 395, "Software Architecture",
                "Cuốn sách khai sinh khái niệm Mẫu thiết kế phần mềm, phân tích sâu sắc 23 mẫu kinh điển giúp giải quyết các vấn đề lặp lại trong kiến trúc phần mềm hướng đối tượng.");
        b3.setShelfLocation("Kệ B1 - Tầng 1");

        Book b4 = new Book("Refactoring", "Addison-Wesley", 2018, 1, "/images/refactor.jpg", "Martin Fowler",
                "9780134757599", 448, "Refactoring",
                "Phương pháp luận chuẩn mực về cải tiến cấu trúc bên trong của mã nguồn mà không thay đổi hành vi bên ngoài, giúp hệ thống luôn linh hoạt trước yêu cầu mới.");
        // b4 không thiết lập vị trí (để trống) nhằm kiểm thử tính năng ẩn/tắt vị trí

        Book b5 = new Book("Head First Java", "O'Reilly", 2022, 8, "/images/hf_java.jpg", "Kathy Sierra",
                "9781491910771", 750, "Education",
                "Phương pháp tiếp cận thị giác trực quan, hài hước và dễ hiểu dành cho người học nhập môn Java, lập trình hướng đối tượng, xử lý đa luồng và mạng máy tính.");
        b5.setShelfLocation("Kệ C1 - Tầng 3");

        Book b6 = new Book("Spring Boot in Action", "Manning", 2018, 5, "/images/spring_action.jpg", "Craig Walls",
                "9781617292545", 264, "Framework",
                "Hướng dẫn toàn diện cách xây dựng ứng dụng web và vi dịch vụ hiện đại bằng Spring Boot, từ tự động cấu hình, Spring Security đến triển khai cloud.");
        b6.setShelfLocation("Kệ C2 - Tầng 3");

        Book b7 = new Book("Dế Mèn Phiêu Lưu Ký", "NXB Kim Đồng", 2020, 12, "/images/demen.jpg", "Tô Hoài",
                "9786042188888", 188, "Văn học thiếu nhi",
                "Kiệt tác văn học thiếu nhi Việt Nam kể về hành trình chu du, trưởng thành của chú Dế Mèn và thông điệp cao đẹp về tình hữu ái, hòa bình và lý tưởng sống tích cực.");
        // b7 không thiết lập vị trí (để trống)

        Magazine m1 = new Magazine("Tạp chí Tin học & Điều khiển", "Viện Hàn Lâm KH&CN", 2026, 8, "/images/cntt.jpg", 1, 3,
                "Số đặc biệt công bố các công trình nghiên cứu mới về Trí tuệ nhân tạo (AI), Hệ thống nhúng tự động hóa và An toàn không gian mạng.");
        m1.setShelfLocation("Kệ Báo - Quầy 1");

        Magazine m2 = new Magazine("National Geographic Vietnam", "NXB Trẻ", 2026, 6, "/images/natgeo.jpg", 45, 6,
                "Ấn bản khám phá thế giới tự nhiên, bảo tồn đa dạng sinh học và các nền văn hóa bản địa độc đáo qua lăng kính nhiếp ảnh phóng sự chuyên sâu.");
        m2.setShelfLocation("Kệ Báo - Quầy 2");

        Magazine m3 = new Magazine("Tạp chí Kinh tế Sài Gòn", "Saigon Times", 2026, 5, "/images/kinhte.jpg", 120, 8,
                "Phân tích chuyên sâu về kinh tế vĩ mô, biến động thị trường tài chính và các chiến lược chuyển đổi số trong khối doanh nghiệp Việt Nam.");
        m3.setShelfLocation("Kệ Báo - Quầy 3");

        List<Document> documents = List.of(b1, b2, b3, b4, b5, b6, b7, m1, m2, m3);
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
     *  - Nếu có file static/images/covers/{ISBN}.jpg trong dự án -> dùng ảnh trong máy
     *     (chạy được cả khi không có mạng).
     *  - Nếu không có file -> dùng link Open Library theo ISBN.
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

    private void seedActivityLogs() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        List<com.library.model.ActivityLog> sampleLogs = List.of(
                com.library.model.ActivityLog.builder()
                        .userId(null)
                        .username("system")
                        .userFullName("Hệ thống tự động")
                        .userRole("SYSTEM")
                        .actionType(com.library.model.ActionType.SYSTEM_INIT)
                        .targetType(com.library.model.TargetType.SYSTEM)
                        .targetId(null)
                        .targetName("Thư Viện Số")
                        .description("Khởi tạo hệ thống dữ liệu thư viện số thành công")
                        .details("Nạp danh mục sách mẫu, người dùng và thiết lập phân quyền ban đầu")
                        .ipAddress("127.0.0.1")
                        .createdAt(now.minusHours(4))
                        .build(),

                com.library.model.ActivityLog.builder()
                        .userId(1L)
                        .username("admin")
                        .userFullName("Quản Trị Viên")
                        .userRole("ROLE_ADMIN")
                        .actionType(com.library.model.ActionType.FILE_IMPORT)
                        .targetType(com.library.model.TargetType.DOCUMENT)
                        .targetId(null)
                        .targetName("sach_nhap_moi_v2.csv")
                        .description("Nhập thành công danh mục sách mới từ tệp CSV")
                        .details("Đã nạp 23 đầu sách chuyên ngành lập trình và công nghệ phần mềm")
                        .ipAddress("192.168.1.10")
                        .createdAt(now.minusHours(3))
                        .build(),

                com.library.model.ActivityLog.builder()
                        .userId(1L)
                        .username("admin")
                        .userFullName("Quản Trị Viên")
                        .userRole("ROLE_ADMIN")
                        .actionType(com.library.model.ActionType.BORROW_CREATE)
                        .targetType(com.library.model.TargetType.BORROW_RECORD)
                        .targetId(1L)
                        .targetName("Clean Code: A Handbook of Agile Software Craftsmanship")
                        .description("Tạo phiếu mượn #1 cho độc giả Nguyễn Văn An")
                        .details("Hạn trả: 14 ngày kể từ ngày mượn")
                        .ipAddress("192.168.1.10")
                        .createdAt(now.minusHours(2))
                        .build(),

                com.library.model.ActivityLog.builder()
                        .userId(1L)
                        .username("admin")
                        .userFullName("Quản Trị Viên")
                        .userRole("ROLE_ADMIN")
                        .actionType(com.library.model.ActionType.BORROW_RETURN)
                        .targetType(com.library.model.TargetType.BORROW_RECORD)
                        .targetId(2L)
                        .targetName("Design Patterns: Elements of Reusable Object-Oriented Software")
                        .description("Hoàn tất tiếp nhận trả sách phiếu mượn #2")
                        .details("Tình trạng sách nguyên vẹn, trả đúng thời hạn")
                        .ipAddress("192.168.1.10")
                        .createdAt(now.minusMinutes(45))
                        .build(),

                com.library.model.ActivityLog.builder()
                        .userId(1L)
                        .username("admin")
                        .userFullName("Quản Trị Viên")
                        .userRole("ROLE_ADMIN")
                        .actionType(com.library.model.ActionType.ROLE_CHANGE)
                        .targetType(com.library.model.TargetType.USER)
                        .targetId(2L)
                        .targetName("Nguyễn Văn An (reader1)")
                        .description("Xác nhận thông tin độc giả kích hoạt tài khoản sử dụng thư viện")
                        .details("Quyền hạn: Độc giả (ROLE_READER)")
                        .ipAddress("192.168.1.10")
                        .createdAt(now.minusMinutes(15))
                        .build()
        );

        activityLogRepository.saveAll(sampleLogs);
    }

    private void seedSystemSettings() {
        LocalDateTime now = LocalDateTime.now();

        List<com.library.model.SystemSetting> defaultSettings = List.of(
                com.library.model.SystemSetting.builder()
                        .settingKey("fine_per_day")
                        .settingValue("5000")
                        .settingGroup("POLICY")
                        .description("Mức phạt trễ hạn mỗi ngày (VNĐ/cuốn/ngày)")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("max_borrow_days")
                        .settingValue("14")
                        .settingGroup("POLICY")
                        .description("Thời hạn mượn sách mặc định (ngày)")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("max_borrow_limit")
                        .settingValue("5")
                        .settingGroup("POLICY")
                        .description("Số lượng sách được mượn tối đa đồng thời")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("qr_request_timeout")
                        .settingValue("30")
                        .settingGroup("POLICY")
                        .description("Thời gian giữ phiếu yêu cầu mượn QR tại quầy (phút)")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("library_opening_hours")
                        .settingValue("Thứ Hai - Thứ Bảy: 08:00 - 21:00 (Nghỉ Chủ Nhật & Ngày lễ)")
                        .settingGroup("INFO")
                        .description("Thời gian mở cửa đón tiếp độc giả")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("library_hotline")
                        .settingValue("024.3854.4444")
                        .settingGroup("INFO")
                        .description("Số điện thoại đường dây nóng hỗ trợ độc giả")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("library_email")
                        .settingValue("hotro@thuvienso.edu.vn")
                        .settingGroup("INFO")
                        .description("Hộp thư điện tử liên hệ thư viện")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("library_regulations_text")
                        .settingValue("1. Xuất trình thẻ thư viện điện tử khi nhận tài liệu tại quầy.\n2. Mỗi độc giả được mượn tối đa 5 cuốn sách trong thời hạn 14 ngày.\n3. Tiền phạt trả trễ hạn là 5.000 VNĐ/cuốn/ngày.\n4. Độc giả có thể tự gia hạn trực tuyến thêm 7 ngày nếu sách chưa quá hạn.\n5. Giữ gìn sách cẩn thận, không làm rách, gập mép hay viết vẽ lên trang sách.")
                        .settingGroup("INFO")
                        .description("Nội dung nội quy chi tiết hiển thị cho bạn đọc")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("feature_online_registration")
                        .settingValue("true")
                        .settingGroup("TOGGLE")
                        .description("Cho phép người dùng tự đăng ký tài khoản trực tuyến")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("feature_qr_borrow")
                        .settingValue("true")
                        .settingGroup("TOGGLE")
                        .description("Kích hoạt tính năng quét mã QR gửi yêu cầu mượn sách")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("feature_self_extension")
                        .settingValue("true")
                        .settingGroup("TOGGLE")
                        .description("Cho phép độc giả tự gia hạn sách trực tuyến trên website")
                        .updatedAt(now)
                        .build(),

                com.library.model.SystemSetting.builder()
                        .settingKey("feature_maintenance_mode")
                        .settingValue("false")
                        .settingGroup("TOGGLE")
                        .description("Chế độ bảo trì hệ thống (chỉ tài khoản quản trị được đăng nhập)")
                        .updatedAt(now)
                        .build()
        );

        systemSettingRepository.saveAll(defaultSettings);
    }

    private void seedNotifications() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        User reader1 = userRepository.findByUsername("reader1").orElse(null);
        User reader2 = userRepository.findByUsername("reader2").orElse(null);

        if (admin == null || reader1 == null) {
            return;
        }

        String broadcastId = java.util.UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        List<com.library.model.Notification> sampleNotifications = new ArrayList<>();

        // Thông báo hệ thống gửi toàn bộ người dùng
        for (User u : List.of(admin, reader1, reader2 != null ? reader2 : reader1)) {
            sampleNotifications.add(
                    com.library.model.Notification.builder()
                            .senderId(admin.getId())
                            .senderName("Ban Quản lý Thư viện")
                            .recipientId(u.getId())
                            .recipientName(u.getFullName())
                            .recipientCode(u.getReaderCode())
                            .broadcastId(broadcastId)
                            .isBroadcast(true)
                            .title("Chào mừng đến với Cổng thông tin Thư viện số")
                            .content("Hệ thống đã nâng cấp giao diện hiện đại, bổ sung tra cứu thời gian thực, quản lý hạn mượn và thông báo đa kênh. Chúc bạn có trải nghiệm đọc sách tuyệt vời!")
                            .type(com.library.model.NotificationType.SYSTEM)
                            .isRead(u.getId().equals(admin.getId()))
                            .targetUrl("/catalog")
                            .build()
            );
        }

        // Thông báo cảnh báo quá hạn cho reader1
        sampleNotifications.add(
                com.library.model.Notification.builder()
                        .senderId(admin.getId())
                        .senderName("Quản lý Thư viện")
                        .recipientId(reader1.getId())
                        .recipientName(reader1.getFullName())
                        .recipientCode(reader1.getReaderCode())
                        .broadcastId(java.util.UUID.randomUUID().toString())
                        .isBroadcast(false)
                        .title("Cảnh báo quá hạn trả sách: Cấu trúc Dữ liệu và Giải thuật")
                        .content("Tài liệu 'Cấu trúc Dữ liệu và Giải thuật' của bạn đã quá hạn mượn. Vui lòng mang sách đến quầy hoàn trả hoặc thực hiện gia hạn để tránh phát sinh thêm phí phạt!")
                        .type(com.library.model.NotificationType.OVERDUE_ALERT)
                        .isRead(false)
                        .targetUrl("/borrow/history")
                        .build()
        );

        // Thông báo cập nhật mượn trả cho reader1
        sampleNotifications.add(
                com.library.model.Notification.builder()
                        .senderId(admin.getId())
                        .senderName("Quản lý Thư viện")
                        .recipientId(reader1.getId())
                        .recipientName(reader1.getFullName())
                        .recipientCode(reader1.getReaderCode())
                        .broadcastId(java.util.UUID.randomUUID().toString())
                        .isBroadcast(false)
                        .title("Tiếp nhận mượn tài liệu thành công")
                        .content("Yêu cầu mượn cuốn sách 'Lập trình Java Cơ bản và Nâng cao' đã được hoàn tất thủ tục. Hạn trả sách là 14 ngày kể từ ngày mượn.")
                        .type(com.library.model.NotificationType.BORROW_UPDATE)
                        .isRead(true)
                        .readAt(now.minusHours(4))
                        .targetUrl("/borrow/history")
                        .build()
        );

        // Thông báo nhắc phí phạt cho reader2 nếu có
        if (reader2 != null) {
            sampleNotifications.add(
                    com.library.model.Notification.builder()
                            .senderId(admin.getId())
                            .senderName("Quản lý Thư viện")
                            .recipientId(reader2.getId())
                            .recipientName(reader2.getFullName())
                            .recipientCode(reader2.getReaderCode())
                            .broadcastId(java.util.UUID.randomUUID().toString())
                            .isBroadcast(false)
                            .title("Nhắc nhở quyết toán phí phạt trễ hạn")
                            .content("Tài khoản của bạn hiện có khoản phí phạt trả trễ chưa thanh toán tại quầy. Vui lòng thanh toán trực tiếp để tiếp tục đăng ký mượn sách mới.")
                            .type(com.library.model.NotificationType.FINE_REMINDER)
                            .isRead(false)
                            .targetUrl("/borrow/history")
                            .build()
            );
        }

        notificationRepository.saveAll(sampleNotifications);
    }
}