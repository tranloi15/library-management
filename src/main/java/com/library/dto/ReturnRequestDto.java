package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDto {
    private Long id;
    private Long fineAmount = 0L;
    private String paymentMethod = "NONE"; // "CASH", "QR_CODE", "NONE"
    private String note;
}
