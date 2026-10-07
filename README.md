# Hệ Thống Quản Lý Thư Viện (Library Management System)

Ứng dụng web quản lý và mượn trả ấn phẩm thư viện được xây dựng trên nền tảng **Java 17** và **Spring Boot 3**. Dự án áp dụng triệt để các nguyên lý của **Lập trình hướng đối tượng (OOP)**, mô hình kiến trúc phân tầng kết hợp **Spring MVC** và cơ chế bảo mật phân quyền theo vai trò (**RBAC**).

---

## 📌 Tính Năng Nổi Bật

### 1. Phân hệ Dành Cho Độc Giả (Reader)
* **Tra cứu tài liệu đa tiêu chí:** Tìm kiếm sách và tạp chí theo từ khóa, tác giả, nhà xuất bản, mã định danh ISBN hoặc thể loại; hỗ trợ chuyển đổi linh hoạt giữa chế độ xem Lưới (Grid) và Bảng (Table).
* **Giỏ mượn cá nhân:** Thêm nhiều tài liệu vào giỏ và gửi yêu cầu mượn trực tuyến.
* **Theo dõi lịch sử mượn trả:** Giám sát trạng thái phiếu mượn (đang mượn, đã trả, quá hạn), số ngày còn lại và hạn trả sách.
* **Thanh toán phí phạt qua VietQR:** Khi phát sinh phí trễ hạn, hệ thống tự động sinh mã QR ngân hàng chuẩn NAPAS VietQR để độc giả thanh toán chuyển khoản nhanh chóng.
* **Hồ sơ cá nhân & Thẻ độc giả số:** Cập nhật thông tin tài khoản, đổi mật khẩu và xuất thẻ thư viện điện tử.

### 2. Phân hệ Dành Cho Quản Trị Viên & Thủ Thư (Admin / Librarian)
* **Bảng điều khiển tổng quan (Dashboard):** Thống kê thời gian thực về tổng số đầu sách, bạn đọc hoạt động, phiếu mượn đang lưu hành và cảnh báo phiếu quá hạn nguy cấp.
* **Quản lý danh mục ấn phẩm:** Thêm mới, cập nhật thông tin sách, tạp chí, số lượng tồn kho và vị trí giá kệ trong kho thư viện.
* **Quản lý mượn trả & Gia hạn:** Tiếp nhận yêu cầu, lập phiếu mượn, xác nhận hoàn trả và kiểm soát thu phí phạt trễ hạn.
* **Nhập / Xuất dữ liệu hàng loạt:**
  * Nhập danh mục sách số lượng lớn từ tệp CSV (Batch Import).
  * Xuất các báo cáo thống kê chuyên sâu ra tệp Excel (`.xlsx`) chuẩn hóa bằng Apache POI.
* **Quản lý tài khoản độc giả:** Danh sách người dùng, kích hoạt hoặc khóa tài khoản vi phạm quy chế.
* **Giám sát nhật ký hoạt động (Audit Trail):** Ghi nhận chi tiết lịch sử các thao tác nghiệp vụ quan trọng trong hệ thống kèm mốc thời gian thực.

---

## 🛠 Công Nghệ Sử Dụng

| Phân tầng | Công nghệ / Thư viện | Mô tả |
| :--- | :--- | :--- |
| **Ngôn ngữ** | Java 17 LTS | Ngôn ngữ hướng đối tượng chủ đạo, kiểu dữ liệu an toàn |
| **Backend Core** | Spring Boot 3.2.3 | Framework phát triển ứng dụng Java doanh nghiệp độc lập |
| **Bảo mật** | Spring Security 6 | Xác thực tài khoản và phân quyền truy cập theo vai trò (RBAC) |
| **Truy xuất CSDL** | Spring Data JPA / Hibernate | Ánh xạ thực thể ORM (Chiến lược kế thừa `InheritanceType.JOINED`) |
| **Cơ sở dữ liệu** | H2 Database / MySQL | CSDL nhúng H2 (chạy ngay không cần cài đặt) hoặc MySQL |
| **Giao diện (Frontend)** | Thymeleaf 3, HTML5, CSS3, JavaScript | Template engine phía máy chủ và xử lý tương tác giao diện |
| **CSS Framework** | Bootstrap 5.3 & Font Awesome 6 | Giao diện chuẩn lưới responsive, tương thích máy tính và điện thoại |
| **Xử lý tệp tính** | Apache POI 5.2.5 | Đọc ghi và xuất báo cáo dữ liệu bảng tính định dạng Excel (`.xlsx`) |
| **Xử lý mã vạch** | ZXing (Zebra Crossing) 3.5.3 | Sinh mã ma trận QR hỗ trợ mượn tự phục vụ và thanh toán VietQR |

---

## 📂 Cấu Trúc Thư Mục Dự Án

```
library-management/
|-- pom.xml                               # Tệp cấu hình thư viện và build Maven
|-- mvnw, mvnw.cmd                        # Bộ công cụ chạy Maven Wrapper tích hợp
|-- src/
    |-- main/
    |   |-- java/com/library/
    |   |   |-- config/                   # Cấu hình bảo mật Spring Security, MVC
    |   |   |-- controller/               # Bộ điều khiển tiếp nhận request và điều phối View
    |   |   |-- dto/                      # Các đối tượng truyền nhận dữ liệu (Form & Stats DTO)
    |   |   |-- model/                    # Các thực thể JPA (BaseEntity, Document, Book,...)
    |   |   |-- repository/               # Interface tầng DAO tương tác với CSDL qua JPA
    |   |   `-- service/                  # Xử lý nghiệp vụ ứng dụng và các lớp ServiceImpl
    |   `-- resources/
    |       |-- application.properties    # Cấu hình cổng, CSDL, JPA, H2 Console
    |       |-- static/                   # Tài nguyên tĩnh: CSS tùy biến, JavaScript, hình ảnh
    |       `-- templates/                # Hệ thống giao diện HTML Thymeleaf phân tầng
    `-- test/                             # Các kịch bản kiểm thử tự động (Unit Test)
```

---

## ⚙️ Yêu Cầu Môi Trường

Trước khi khởi chạy dự án, hãy đảm bảo máy tính đã cài đặt:
* **Java Development Kit (JDK):** Phiên bản **17** hoặc **21 LTS**.
* **Git:** Để sao chép mã nguồn từ kho lưu trữ.
* **Trình duyệt web hiện đại:** Google Chrome, Microsoft Edge hoặc Mozilla Firefox.
* *(Tùy chọn)* **Apache Maven 3.8+** (nếu không sử dụng Maven Wrapper kèm theo).

---

## 🚀 Hướng Dẫn Cài Đặt Và Khởi Chạy

### Bước 1: Sao chép mã nguồn (Clone repository)
Mở cửa sổ dòng lệnh (Terminal / PowerShell / CMD) và thực hiện lệnh:
```bash
git clone https://github.com/tranloi15/library-management.git
cd library-management
```

### Bước 2: Biên dịch và chạy ứng dụng

Dự án đã tích hợp sẵn **Maven Wrapper**, bạn không cần cài đặt Maven trước:

* **Trên hệ điều hành Windows (PowerShell / Command Prompt):**
  ```cmd
  .\mvnw.cmd spring-boot:run
  ```

* **Trên hệ điều hành Linux / macOS:**
  ```bash
  chmod +x mvnw
  ./mvnw spring-boot:run
  ```

> Quá trình khởi động sẽ tự động tải các dependency cần thiết, cấu hình cơ sở dữ liệu nhúng H2 trong bộ nhớ và nạp sẵn dữ liệu mẫu qua `DataSeeder`.

### Bước 3: Truy cập hệ thống
Sau khi màn hình thông báo `Started LibraryApplication in ... seconds`:
* Mở trình duyệt web và truy cập địa chỉ: [http://localhost:8080](http://localhost:8080)
* Truy cập giao diện quản trị CSDL H2 Console (nếu cần): [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  * **JDBC URL:** `jdbc:h2:mem:librarydb`
  * **User Name:** `sa`
  * **Password:** *(để trống)*

---

## 🔑 Tài Khoản Thử Nghiệm Mặc Định

Hệ thống đã tự động tạo sẵn các tài khoản demo để trải nghiệm đầy đủ các phân quyền:

| Vai trò | Tên đăng nhập / Email | Mật khẩu | Phạm vi quyền hạn |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | `123456` | Toàn quyền quản trị hệ thống, danh mục sách, độc giả, báo cáo và audit log |
| **Độc giả mẫu (Reader)** | `reader4@gmail.com` | `123456` | Tra cứu sách, quản lý giỏ mượn cá nhân, xem lịch sử và thanh toán phạt |

---

## 💻 Hướng Dẫn Chạy Trên Các Môi Trường IDE

Dự án tương thích và vận hành thuận lợi trên các công cụ lập trình Java phổ biến:

* **IntelliJ IDEA (Khuyến nghị):**
  1. Chọn `File` $\rightarrow$ `Open...` và trỏ đến thư mục `library-management`.
  2. IntelliJ sẽ tự động nhận diện tệp `pom.xml` và tải các thư viện.
  3. Mở tệp `src/main/java/com/library/LibraryApplication.java` và nhấn biểu tượng **Run** (tam giác màu xanh).
* **Visual Studio Code:**
  1. Cài đặt gói mở rộng **Extension Pack for Java** và **Spring Boot Extension Pack**.
  2. Mở thư mục dự án trên VS Code.
  3. Nhấn tổ hợp phím `F5` hoặc mở mục Spring Boot Dashboard và chọn **Run**.
* **Apache NetBeans:**
  1. Chọn `File` $\rightarrow$ `Open Project` và mở thư mục chứa mã nguồn (NetBeans hỗ trợ trực tiếp các dự án Maven).
  2. Nhấp chuột phải vào dự án và chọn **Run**.

---

## 📄 Bản Quyền & Giấy Phép
Dự án được xây dựng phục vụ mục đích học tập và nghiên cứu thực hành môn học Lập trình hướng đối tượng. Mọi đóng góp và mã nguồn mở tuân thủ giấy phép tự do sử dụng trong giáo dục.
