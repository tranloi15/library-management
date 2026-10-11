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

    private String isbn;

    private String imageUrl;

        public TopBookDto(Long bookId, String title, String author, Long borrowCount) {
        this(bookId, title, author, borrowCount, null, null);
    }

    public String getPrimaryCoverUrl() {
        if (isbn != null && !isbn.isBlank()) {
            return "/images/covers/" + isbn.trim() + ".jpg";
        }
        return getFallbackCoverUrl();
    }

    public String getFallbackCoverUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        return DEFAULT_COVER;
    }
}