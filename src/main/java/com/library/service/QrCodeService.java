package com.library.service;

import com.library.model.Document;

import java.io.IOException;
import java.util.List;

public interface QrCodeService {

    byte[] generateQrCodeImage(String text, int width, int height) throws IOException;

    byte[] generateDocumentQrCode(Document document, int width, int height) throws IOException;

    byte[] generateBulkQrZip(List<Document> documents) throws IOException;
}
