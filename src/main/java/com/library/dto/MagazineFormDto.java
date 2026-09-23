package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MagazineFormDto {
    private Long id;

    @NotBlank(message = "Tên tạp chí không được để trống")
    private String title;

    @NotBlank(message = "Nhà xuất bản không được để trống")
    private String publisher;

    @NotNull(message = "Số phát hành không được để trống")
    @Min(value = 1, message = "Số phát hành phải > 0")
    private Integer issueNumber;

    @NotNull(message = "Tháng phát hành không được để trống")
    @Min(value = 1, message = "Tháng phát hành từ 1 đến 12")
    @Max(value = 12, message = "Tháng phát hành từ 1 đến 12")
    private Integer publishMonth;

    @NotNull(message = "Năm phát hành không được để trống")
    @Min(value = 1900, message = "Năm phát hành không hợp lệ")
    private Integer publishYear;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tồn kho phải > 0")
    private Integer quantity;

    private String imageUrl;
}