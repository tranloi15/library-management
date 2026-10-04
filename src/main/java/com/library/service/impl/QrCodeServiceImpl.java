package com.library.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.library.model.Document;
import com.library.service.QrCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Override
    public byte[] generateQrCodeImage(String text, int width, int height) throws IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (WriterException e) {
            log.error("Lỗi khi tạo mã QR: {}", e.getMessage());
            throw new IOException("Không thể tạo mã QR", e);
        }
    }

    @Override
    public byte[] generateDocumentQrCode(Document document, int width, int height) throws IOException {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("Tài liệu không hợp lệ để tạo mã QR");
        }
        // Mã hóa đường dẫn yêu cầu mượn tự phục vụ
        String payload = "/borrow/qr-request?bookId=" + document.getId();
        return generateQrCodeImage(payload, width, height);
    }

    @Override
    public byte[] generateBulkQrZip(List<Document> documents) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Document doc : documents) {
                if (doc == null || doc.getId() == null) {
                    continue;
                }
                byte[] qrBytes = generateDocumentQrCode(doc, 350, 350);
                String safeTitle = toSafeFileName(doc.getTitle());
                String fileName = String.format("BK_%04d_%s.png", doc.getId(), safeTitle);

                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(qrBytes);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private String toSafeFileName(String input) {
        if (input == null || input.isBlank()) {
            return "book";
        }
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        String clean = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        clean = clean.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_");
        if (clean.length() > 30) {
            clean = clean.substring(0, 30);
        }
        return clean.toLowerCase();
    }
}
