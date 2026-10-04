package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object đóng gói dữ liệu thủ tục hoàn trả tài liệu và thu phí/phụ phí tại quầy.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDto {

    private Long id;
    private Long fineAmount = 0L;
    private Long extraFee = 0L;
    private String extraFeeReason;
    private String paymentMethod = "NONE";
    private String note;

    public long getEffectiveFine() {
        return (fineAmount != null && fineAmount > 0) ? fineAmount : 0L;
    }

    public long getEffectiveExtraFee() {
        return (extraFee != null && extraFee > 0) ? extraFee : 0L;
    }

    public long getTotalPayment() {
        return getEffectiveFine() + getEffectiveExtraFee();
    }

    public String buildAuditNote() {
        StringBuilder fullNote = new StringBuilder();
        long extra = getEffectiveExtraFee();
        if (extra > 0) {
            fullNote.append("[Phụ phí: ").append(String.format(java.util.Locale.US, "%,d", extra)).append(" VNĐ");
            if (extraFeeReason != null && !extraFeeReason.isBlank()) {
                fullNote.append(" - Lý do: ").append(extraFeeReason.trim());
            }
            fullNote.append("] ");
        }
        if (note != null && !note.isBlank()) {
            fullNote.append(note.trim());
        }
        return fullNote.toString().trim();
    }
}
