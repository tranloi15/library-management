package com.library.controller;

import com.library.service.BookFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/documents")
@RequiredArgsConstructor
public class BookFileController {

    private final BookFileService bookFileService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportBooks() {

        try {

            byte[] file = bookFileService.exportBooks();

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
            );

            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("library_books.xlsx")
                            .build()
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(file);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Không thể xuất danh sách sách.",
                    e
            );
        }
    }

    @PostMapping("/import")
    public String importBooks(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {

            int importedCount =
                    bookFileService.importBooks(file);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Nhập thành công " +
                            importedCount +
                            " sách."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

        } catch (IOException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể đọc file CSV."
            );
        }

        return "redirect:/dashboard";
    }
}