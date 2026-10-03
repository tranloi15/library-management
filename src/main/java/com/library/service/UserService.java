package com.library.service;

import com.library.model.RoleName;
import com.library.model.User;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createReader(User reader) {
        if (reader.getEmail() == null || reader.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống!");
        }

        String email = reader.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã tồn tại trong hệ thống!");
        }

        reader.setUsername(email);
        reader.setEmail(email);
        reader.setRole(RoleName.ROLE_READER);
        reader.setActive(true);

        String rawPassword = (reader.getPassword() != null && !reader.getPassword().trim().isEmpty())
                ? reader.getPassword().trim()
                : "123456";
        reader.setPassword(passwordEncoder.encode(rawPassword));

        return userRepository.save(reader);
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        user.setFullName(userDetails.getFullName());
        user.setPhone(userDetails.getPhone());
        user.setAddress(userDetails.getAddress());
        user.setDateOfBirth(userDetails.getDateOfBirth());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác!");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }

    @Transactional
    public void updateRole(Long id, RoleName newRole) {
        User user = getUserById(id);
        if (user.getUsername().equals("admin")) {
            throw new IllegalArgumentException("Không thể thay đổi quyền của tài khoản Admin gốc!");
        }
        user.setRole(newRole);
        userRepository.save(user);
    }

    // ===== FORGOT PASSWORD =====

    /**
     * Tạo token reset mật khẩu cho user (giả lập gửi email, thực tế trả token về để hiển thị).
     * Trong môi trường thực tế: gửi email chứa link reset.
     * @return token nếu email hợp lệ, null nếu không tìm thấy
     */
    @Transactional
    public String generatePasswordResetToken(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase()).map(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            return token;
        }).orElse(null);
    }

    public boolean isValidResetToken(String token) {
        return userRepository.findByPasswordResetToken(token)
                .map(u -> u.getPasswordResetTokenExpiry() != null &&
                          u.getPasswordResetTokenExpiry().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ!"));

        if (user.getPasswordResetTokenExpiry() == null ||
                user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token đã hết hạn! Vui lòng yêu cầu lại.");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    // ===== QUERIES =====

    public List<User> getAllReaders() {
        return userRepository.findByRole(RoleName.ROLE_READER);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> searchReaders(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllReaders();
        }
        return userRepository.searchUsersByRole(RoleName.ROLE_READER, keyword.trim());
    }

    public List<User> searchAllUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers();
        }
        return userRepository.searchAllUsers(keyword.trim());
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));
    }

    @Transactional
    public void toggleActiveStatus(Long id) {
        User user = getUserById(id);
        if (user.getUsername().equals("admin")) {
            throw new IllegalArgumentException("Không thể khóa tài khoản Admin gốc!");
        }
        user.setActive(!user.isActive());
        userRepository.save(user);
    }
}