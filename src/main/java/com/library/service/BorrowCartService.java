package com.library.service;

import com.library.model.Document;
import com.library.model.User;
import com.library.model.BorrowRecord;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

public interface BorrowCartService {

    String SESSION_KEY = "SESSION_BORROW_CART";

    List<Long> getCartBookIds(HttpSession session);

    List<Document> getCartItems(HttpSession session);

    int getCartCount(HttpSession session);

    int getAvailableQuota(User user);

    int getActiveBorrowCount(User user);

    Map<String, Object> addToCart(HttpSession session, Long bookId, User user);

    void removeFromCart(HttpSession session, Long bookId);

    void clearCart(HttpSession session);

    List<BorrowRecord> checkout(HttpSession session, User user);
}
