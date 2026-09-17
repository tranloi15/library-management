package com.library.service;

import com.library.model.RoleName;
import com.library.model.User;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        // Tự tạo tài khoản đăng nhập mặc định: Username = Email, Password mặc định = 123456
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

    public List<User> getAllReaders() {
        return userRepository.findByRole(RoleName.ROLE_READER);
    }

    public List<User> searchReaders(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllReaders();
        }
        return userRepository.searchUsersByRole(RoleName.ROLE_READER, keyword.trim());
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
        user.setActive(!user.isActive());
        userRepository.save(user);
    }
}