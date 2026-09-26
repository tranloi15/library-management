package com.library.dto;

import com.library.model.BookCondition;

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

    private Long damageFee = 0L;

    private BookCondition bookCondition = BookCondition.GOOD;

    private String paymentMethod = "NONE";

    private String note;
}