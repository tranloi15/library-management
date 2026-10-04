package com.library.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Kiểm thử logic đóng gói trong ReturnRequestDto")
class ReturnRequestDtoTest {

    @Test
    @DisplayName("Kiểm thử tính tổng tiền thanh toán: tiền phạt + phụ phí")
    void testTotalPaymentCalculation() {
        ReturnRequestDto dto = new ReturnRequestDto();
        dto.setFineAmount(15000L);
        dto.setExtraFee(5000L);
        dto.setExtraFeeReason("Bìa sách bị rách");

        assertEquals(20000L, dto.getTotalPayment());
        assertEquals("[Phụ phí: 5,000 VNĐ - Lý do: Bìa sách bị rách]", dto.buildAuditNote());
    }

    @Test
    @DisplayName("Kiểm thử khi không có phụ phí và không có ghi chú")
    void testNoExtraFeeAndNoNote() {
        ReturnRequestDto dto = new ReturnRequestDto();
        dto.setFineAmount(10000L);

        assertEquals(10000L, dto.getTotalPayment());
        assertEquals("", dto.buildAuditNote());
    }

    @Test
    @DisplayName("Kiểm thử khi có cả phụ phí và ghi chú của thủ thư")
    void testExtraFeeAndCustomNote() {
        ReturnRequestDto dto = new ReturnRequestDto();
        dto.setFineAmount(0L);
        dto.setExtraFee(2000L);
        dto.setExtraFeeReason("Gập mép trang sách");
        dto.setNote("Độc giả cam kết giữ gìn lần sau");

        assertEquals(2000L, dto.getTotalPayment());
        assertEquals("[Phụ phí: 2,000 VNĐ - Lý do: Gập mép trang sách] Độc giả cam kết giữ gìn lần sau", dto.buildAuditNote());
    }
}
