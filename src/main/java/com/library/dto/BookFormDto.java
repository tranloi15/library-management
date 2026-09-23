package com.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookFormDto {
    private Long id;

    @NotBlank(message = "Tên sách không được để trống")
    private String title;

    @NotBlank(message = "Tác giả không được để trống")
    private String author;

    @NotBlank(message = "Nhà xuất bản không được để trống")
    private String publisher;

    @NotBlank(message = "Mã ISBN không được để trống")
    private String isbn;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tồn kho phải > 0")
    private Integer quantity;

    @NotNull(message = "Số trang không được để trống")
    @Min(value = 1, message = "Số trang phải > 0")
    private Integer pageCount;

    @NotNull(message = "Năm phát hành không được để trống")
    @Min(value = 1900, message = "Năm phát hành không hợp lệ")
    private Integer publishYear;

    private String genre;
    private String imageUrl;
}