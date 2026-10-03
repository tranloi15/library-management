package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopBookDto {

    private static final String DEFAULT_COVER = "/images/default-book.svg";

    private Long bookId;

    private String title;

    private String author;

    private Long borrowCount;

    /** Mã ISBN, dùng để tìm ảnh bìa trong /images/covers/{ISBN}.jpg */
    private String isbn;

    /** Ảnh bìa lưu trong database (ví dụ ảnh của sách mẫu /images/clean_code.jpg) */
    private String imageUrl;

    /**
     * Giữ lại constructor 4 tham số cũ để các câu truy vấn
     * "SELECT new com.library.dto.TopBookDto(...)" hiện có không bị lỗi.
     */
    public TopBookDto(Long bookId, String title, String author, Long borrowCount) {
        this(bookId, title, author, borrowCount, null, null);
    }

    /** Ảnh bìa ưu tiên: file trong máy theo ISBN. Không có ISBN thì dùng ảnh dự phòng. */
    public String getPrimaryCoverUrl() {
        if (isbn != null && !isbn.isBlank()) {
            return "/images/covers/" + isbn.trim() + ".jpg";
        }
        return getFallbackCoverUrl();
    }

    /** Ảnh dự phòng: imageUrl trong database, nếu trống thì dùng bìa mặc định. */
    public String getFallbackCoverUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        return DEFAULT_COVER;
    }
}