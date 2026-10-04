package com.library.service;

import com.library.model.ActionType;
import com.library.model.ActivityLog;
import com.library.model.RoleName;
import com.library.model.TargetType;
import com.library.model.User;
import com.library.repository.ActivityLogRepository;
import com.library.repository.UserRepository;
import com.library.service.impl.ActivityLogServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử dịch vụ ghi nhật ký hoạt động ActivityLogService")
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityLogServiceImpl activityLogService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Ghi log khi không có người dùng đăng nhập thì gán mặc định là hệ thống")
    void testLogWithoutAuthentication() {
        activityLogService.log(
                ActionType.SYSTEM_INIT,
                TargetType.SYSTEM,
                null,
                "Thư Viện Số",
                "Khởi tạo hệ thống dữ liệu"
        );

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository, times(1)).save(captor.capture());

        ActivityLog saved = captor.getValue();
        assertNotNull(saved);
        assertEquals("system", saved.getUsername());
        assertEquals("Hệ thống tự động", saved.getUserFullName());
        assertEquals("SYSTEM", saved.getUserRole());
        assertEquals(ActionType.SYSTEM_INIT, saved.getActionType());
        assertEquals(TargetType.SYSTEM, saved.getTargetType());
        assertEquals("Khởi tạo hệ thống dữ liệu", saved.getDescription());
    }

    @Test
    @DisplayName("Ghi log khi người dùng đã đăng nhập thì tự động lấy đúng thông tin tài khoản và vai trò")
    void testLogWithAuthenticatedUser() {
        User mockUser = new User("admin", "123456", "Quản Trị Viên", "admin@library.com", "0988000111", "Hà Nội", RoleName.ROLE_ADMIN);
        mockUser.setId(10L);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("admin", "123456", java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        activityLogService.log(
                ActionType.BOOK_CREATE,
                TargetType.DOCUMENT,
                55L,
                "Clean Architecture",
                "Thêm mới đầu sách: Clean Architecture",
                "ISBN: 9780134494166"
        );

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository, times(1)).save(captor.capture());

        ActivityLog saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(10L, saved.getUserId());
        assertEquals("admin", saved.getUsername());
        assertEquals("Quản Trị Viên", saved.getUserFullName());
        assertEquals("ROLE_ADMIN", saved.getUserRole());
        assertEquals(ActionType.BOOK_CREATE, saved.getActionType());
        assertEquals("Clean Architecture", saved.getTargetName());
        assertEquals("ISBN: 9780134494166", saved.getDetails());
    }

    @Test
    @DisplayName("Kiểm tra phương thức logSystem luôn gán người thực hiện là system")
    void testLogSystem() {
        activityLogService.logSystem(
                ActionType.SYSTEM_INIT,
                TargetType.SYSTEM,
                null,
                "CronJob",
                "Tiến trình kiểm tra trễ hạn tự động",
                "Cập nhật 3 phiếu quá hạn"
        );

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository, times(1)).save(captor.capture());

        ActivityLog saved = captor.getValue();
        assertNotNull(saved);
        assertEquals("system", saved.getUsername());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals(ActionType.SYSTEM_INIT, saved.getActionType());
    }
}
