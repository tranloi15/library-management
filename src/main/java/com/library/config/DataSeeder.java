package com.library.config;

import com.library.model.*;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode("123456");

        User admin = new User("admin", encodedPassword, "Quản Trị Viên", "admin@library.com", "0988000111", "Hà Nội",
                LocalDate.of(1990, 5, 15), RoleName.ROLE_ADMIN);
        User reader1 = new User("reader1@gmail.com", encodedPassword, "Nguyễn Văn An", "reader1@gmail.com",
                "0912345678", "Hà Nội", LocalDate.of(1998, 8, 20), RoleName.ROLE_READER);
        User reader2 = new User("reader2@gmail.com", encodedPassword, "Trần Thị Bình", "reader2@gmail.com",
                "0923456789", "Đà Nẵng", LocalDate.of(2001, 12, 10), RoleName.ROLE_READER);

        userRepository.saveAll(List.of(admin, reader1, reader2));
    }

    private void seedDocuments() {
        List<Document> documents = List.of(
                // 7 Sách
                new Book("Clean Code", "Prentice Hall", 2008, 10, "/images/clean_code.jpg", "Robert C. Martin",
                        "9780132350884", 464, "Software Engineering",
                        "Tác phẩm kinh điển của Uncle Bob hướng dẫn triết lý, nguyên tắc và kỹ năng viết mã nguồn sạch, dễ đọc, dễ bảo trì và giảm thiểu tối đa nợ kỹ thuật cho lập trình viên."),
                new Book("Effective Java", "Addison-Wesley", 2018, 5, "/images/effective_java.jpg", "Joshua Bloch",
                        "9780134685991", 416, "Programming",
                        "Cẩm nang gối đầu giường của cựu kỹ sư trưởng Java Joshua Bloch, bao gồm 90 nguyên tắc vàng thiết kế lớp, quản lý tài nguyên, generics và lập trình bất đồng bộ tối ưu."),
                new Book("Design Patterns", "Addison-Wesley", 1994, 4, "/images/gof.jpg", "Erich Gamma et al.",
                        "9780201633610", 395, "Software Architecture",
                        "Cuốn sách khai sinh khái niệm Mẫu thiết kế phần mềm, phân tích sâu sắc 23 mẫu kinh điển giúp giải quyết các vấn đề lặp lại trong kiến trúc phần mềm hướng đối tượng."),
                new Book("Refactoring", "Addison-Wesley", 2018, 6, "/images/refactor.jpg", "Martin Fowler",
                        "9780134757599", 448, "Refactoring",
                        "Phương pháp luận chuẩn mực về cải tiến cấu trúc bên trong của mã nguồn mà không thay đổi hành vi bên ngoài, giúp hệ thống luôn linh hoạt trước yêu cầu mới."),
                new Book("Head First Java", "O'Reilly", 2022, 12, "/images/hf_java.jpg", "Kathy Sierra",
                        "9781491910771", 750, "Education",
                        "Phương pháp tiếp cận thị giác trực quan, hài hước và dễ hiểu dành cho người học nhập môn Java, lập trình hướng đối tượng, xử lý đa luồng và mạng máy tính."),
                new Book("Spring Boot in Action", "Manning", 2018, 8, "/images/spring_action.jpg", "Craig Walls",
                        "9781617292545", 264, "Framework",
                        "Hướng dẫn toàn diện cách xây dựng ứng dụng web và vi dịch vụ hiện đại bằng Spring Boot, từ tự động cấu hình, Spring Security đến triển khai cloud."),
                new Book("Dế Mèn Phiêu Lưu Ký", "NXB Kim Đồng", 2020, 20, "/images/demen.jpg", "Tô Hoài",
                        "9786042188888", 188, "Văn học thiếu nhi",
                        "Kiệt tác văn học thiếu nhi Việt Nam kể về hành trình chu du, trưởng thành của chú Dế Mèn và thông điệp cao đẹp về tình hữu ái, hòa bình và lý tưởng sống tích cực."),

                // 3 Tạp chí
                new Magazine("Tạp chí Tin học & Điều khiển", "Viện Hàn Lâm KH&CN", 2026, 15, "/images/cntt.jpg", 1, 3,
                        "Số đặc biệt công bố các công trình nghiên cứu mới về Trí tuệ nhân tạo (AI), Hệ thống nhúng tự động hóa và An toàn không gian mạng."),
                new Magazine("National Geographic Vietnam", "NXB Trẻ", 2026, 10, "/images/natgeo.jpg", 45, 6,
                        "Ấn bản khám phá thế giới tự nhiên, bảo tồn đa dạng sinh học và các nền văn hóa bản địa độc đáo qua lăng kính nhiếp ảnh phóng sự chuyên sâu."),
                new Magazine("Tạp chí Kinh tế Sài Gòn", "Saigon Times", 2026, 8, "/images/kinhte.jpg", 120, 8,
                        "Phân tích chuyên sâu về kinh tế vĩ mô, biến động thị trường tài chính và các chiến lược chuyển đổi số trong khối doanh nghiệp Việt Nam."));

        documentRepository.saveAll(documents);
    }

    private void seedBorrowRecords() {
        // Độc giả reader1 (id thường là 2) mượn sách
        User reader1 = userRepository.findByUsername("reader1@gmail.com").orElse(null);
        List<Document> docs = documentRepository.findAll();

        if (reader1 != null && docs.size() >= 3) {
            LocalDate today = LocalDate.now();

            // 1. Sách đang mượn, còn hạn 9 ngày
            BorrowRecord b1 = new BorrowRecord(
                    docs.get(0), // Clean Code
                    reader1,
                    today.minusDays(5),
                    today.plusDays(9),
                    BorrowStatus.BORROWING);

            // 2. Sách đang mượn, đã quá hạn 6 ngày
            BorrowRecord b2 = new BorrowRecord(
                    docs.get(4), // Head First Java
                    reader1,
                    today.minusDays(20),
                    today.minusDays(6),
                    BorrowStatus.BORROWING);

            // 3. Sách đã trả
            BorrowRecord b3 = new BorrowRecord(
                    docs.get(1), // Effective Java
                    reader1,
                    today.minusDays(30),
                    today.minusDays(16),
                    today.minusDays(18),
                    BorrowStatus.RETURNED);

            borrowRecordRepository.saveAll(List.of(b1, b2, b3));
        }
    }
}