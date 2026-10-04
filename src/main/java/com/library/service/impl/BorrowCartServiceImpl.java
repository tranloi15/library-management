package com.library.service.impl;

import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.model.User;
import com.library.repository.DocumentRepository;
import com.library.service.BorrowCartService;
import com.library.service.BorrowService;
import com.library.service.SettingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BorrowCartServiceImpl implements BorrowCartService {

    private final DocumentRepository documentRepository;
    private final BorrowService borrowService;
    private final SettingService settingService;

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> getCartBookIds(HttpSession session) {
        if (session == null) {
            return new ArrayList<>();
        }
        Object cartObj = session.getAttribute(SESSION_KEY);
        if (cartObj instanceof List<?>) {
            return (List<Long>) cartObj;
        }
        List<Long> newCart = new ArrayList<>();
        session.setAttribute(SESSION_KEY, newCart);
        return newCart;
    }

    @Override
    public List<Document> getCartItems(HttpSession session) {
        List<Long> ids = getCartBookIds(session);
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Document> result = new ArrayList<>();
        for (Long id : ids) {
            documentRepository.findById(id).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public int getCartCount(HttpSession session) {
        return getCartBookIds(session).size();
    }

    @Override
    public int getActiveBorrowCount(User user) {
        if (user == null) return 0;
        List<BorrowRecord> records = borrowService.getUserHistory(user.getId());
        return (int) records.stream()
                .filter(r -> !r.isOverdue() && r.getStatus() != BorrowStatus.RETURNED && r.getStatus() != BorrowStatus.CANCELLED && r.getStatus() != BorrowStatus.REJECTED)
                .count();
    }

    @Override
    public int getAvailableQuota(User user) {
        if (user == null) return 0;
        List<BorrowRecord> records = borrowService.getUserHistory(user.getId());
        boolean hasOverdue = records.stream().anyMatch(BorrowRecord::isOverdue);
        if (hasOverdue) {
            return 0;
        }
        int maxLimit = settingService != null ? settingService.getMaxBorrowLimit() : 5;
        int activeCount = getActiveBorrowCount(user);
        return Math.max(0, maxLimit - activeCount);
    }

    @Override
    public Map<String, Object> addToCart(HttpSession session, Long bookId, User user) {
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để thêm tài liệu vào giỏ mượn!");
            return response;
        }

        Document document = documentRepository.findById(bookId).orElse(null);
        if (document == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy cuốn sách này trong hệ thống thư viện!");
            return response;
        }

        if (!document.isBorrowable()) {
            response.put("success", false);
            response.put("message", "Tạp chí chỉ phục vụ đọc tại chỗ tại phòng đọc thư viện, không áp dụng mượn về!");
            return response;
        }

        if (document.getQuantity() <= 0 || !document.isAvailable()) {
            response.put("success", false);
            response.put("message", "Cuốn sách này hiện đã hết bản khả dụng trong kho!");
            return response;
        }

        List<BorrowRecord> userRecords = borrowService.getUserHistory(user.getId());
        boolean hasOverdue = userRecords.stream().anyMatch(BorrowRecord::isOverdue);
        if (hasOverdue) {
            response.put("success", false);
            response.put("message", "Bạn đang có tài liệu quá hạn trả. Vui lòng hoàn trả sách tại quầy trước khi mượn tiếp!");
            return response;
        }

        // Kiểm tra độc giả có đang mượn cuốn sách này không
        boolean alreadyBorrowing = userRecords.stream()
                .anyMatch(r -> r.getBookId().equals(bookId) && (r.getStatus() == BorrowStatus.BORROWING || r.getStatus() == BorrowStatus.PENDING));
        if (alreadyBorrowing) {
            response.put("success", false);
            response.put("message", "Bạn đã gửi yêu cầu mượn hoặc đang giữ cuốn sách này rồi!");
            return response;
        }

        List<Long> cartIds = getCartBookIds(session);
        if (cartIds.contains(bookId)) {
            response.put("success", false);
            response.put("message", "Cuốn sách này đã có trong giỏ mượn của bạn!");
            return response;
        }

        int maxLimit = settingService != null ? settingService.getMaxBorrowLimit() : 5;
        int activeCount = getActiveBorrowCount(user);
        int availableQuota = Math.max(0, maxLimit - activeCount);

        if (availableQuota <= 0) {
            response.put("success", false);
            response.put("message", "Bạn đã mượn tối đa " + maxLimit + " cuốn. Vui lòng trả bớt sách trước khi thêm vào giỏ!");
            return response;
        }

        if (cartIds.size() >= availableQuota) {
            response.put("success", false);
            response.put("message", "Bạn chỉ có thể thêm tối đa " + availableQuota + " cuốn vào giỏ mượn (đang giữ " + activeCount + "/" + maxLimit + " cuốn chưa trả)!");
            return response;
        }

        cartIds.add(bookId);
        session.setAttribute(SESSION_KEY, cartIds);

        response.put("success", true);
        response.put("message", "Đã thêm vào giỏ! Ấn vào xem giỏ mượn để xem chi tiết và xác nhận mượn.");
        response.put("cartCount", cartIds.size());
        response.put("availableQuota", availableQuota);
        response.put("activeCount", activeCount);
        return response;
    }

    @Override
    public void removeFromCart(HttpSession session, Long bookId) {
        List<Long> cartIds = getCartBookIds(session);
        cartIds.remove(bookId);
        session.setAttribute(SESSION_KEY, cartIds);
    }

    @Override
    public void clearCart(HttpSession session) {
        if (session != null) {
            session.setAttribute(SESSION_KEY, new ArrayList<Long>());
        }
    }

    @Override
    @Transactional
    public List<BorrowRecord> checkout(HttpSession session, User user) {
        List<Long> cartIds = new ArrayList<>(getCartBookIds(session));
        if (cartIds.isEmpty()) {
            throw new IllegalStateException("Giỏ mượn của bạn đang trống!");
        }

        List<BorrowRecord> createdRecords = new ArrayList<>();
        for (Long bookId : cartIds) {
            BorrowRecord record = borrowService.createPendingQrRequest(user.getId(), bookId);
            createdRecords.add(record);
        }

        clearCart(session);
        return createdRecords;
    }
}
