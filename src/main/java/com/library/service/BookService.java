package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + id));
    }

    public boolean existsByIsbn(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }

    @Transactional
    public Book saveBook(Book book) {
        if (book.getIsbn() != null && book.getId() == null && bookRepository.existsByIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("Mã ISBN '" + book.getIsbn() + "' đã tồn tại!");
        }
        return bookRepository.save(book);
    }
}
