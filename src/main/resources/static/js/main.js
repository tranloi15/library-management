/**
 * HỆ THỐNG QUẢN LÝ THƯ VIỆN - CLIENT SCRIPTS
 * Phụ trách: Hưng (Khung Giao diện Master Layout & UX)
 */

document.addEventListener('DOMContentLoaded', function () {
    try { initThemeToggle(); } catch (e) { console.error('Theme toggle error:', e); }
    try { initMobileSidebar(); } catch (e) { console.error('Mobile sidebar error:', e); }
    try { initSidebarActiveState(); } catch (e) { console.error('Sidebar active state error:', e); }
    try { initViewModeToggle(); } catch (e) { console.error('View mode error:', e); }
    try { initDeleteConfirmModal(); } catch (e) { console.error('Delete modal error:', e); }
    try { initAutoHideAlerts(); } catch (e) { console.error('Alerts error:', e); }
    try { initKeyboardShortcuts(); } catch (e) { console.error('Shortcuts error:', e); }

    // Dọn dẹp trạng thái bảng màu cũ nếu có, luôn giữ mặc định Sapphire Tech
    localStorage.removeItem('library_palette');
    document.documentElement.removeAttribute('data-theme-palette');
    document.documentElement.removeAttribute('data-theme-style');
});

/* ==========================================================================
   1. THEME TOGGLE (LIGHT / DARK SLATE)
   ========================================================================== */
function initThemeToggle() {
    const themeCheckbox = document.getElementById('themeToggleCheckbox');
    const themeLabel = document.getElementById('themeLabel');
    const themeIcon = document.getElementById('themeIcon');
    const switchWrapper = document.querySelector('.theme-switch-wrapper');

    // Đọc theme đã lưu, mặc định là light
    const currentTheme = localStorage.getItem('library_theme') || 'light';
    applyTheme(currentTheme);

    if (themeCheckbox) {
        themeCheckbox.checked = (currentTheme === 'dark');
        themeCheckbox.addEventListener('change', function () {
            const newTheme = this.checked ? 'dark' : 'light';
            localStorage.setItem('library_theme', newTheme);
            applyTheme(newTheme);
        });
    }

    if (switchWrapper) {
        // Ngăn sự kiện click làm đóng menu dropdown của Bootstrap
        switchWrapper.addEventListener('click', function (e) {
            e.stopPropagation();
        });
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-bs-theme', theme);
        if (document.body) {
            document.body.setAttribute('data-bs-theme', theme);
        }

        if (themeLabel) {
            themeLabel.textContent = (theme === 'dark') ? 'Nền tối' : 'Nền sáng';
        }
        if (themeIcon) {
            if (theme === 'dark') {
                themeIcon.className = 'bi bi-moon-stars text-warning fs-6';
            } else {
                themeIcon.className = 'bi bi-sun text-muted fs-6';
            }
        }
        if (themeCheckbox) {
            themeCheckbox.checked = (theme === 'dark');
        }
    }
}

/* ==========================================================================
   2. SIDEBAR TOGGLE (THU NHỎ TRÊN DESKTOP & MENU DRAWER TRÊN MOBILE)
   ========================================================================== */
function initMobileSidebar() {
    const toggleBtn = document.getElementById('btnToggleSidebar');
    const closeBtn = document.getElementById('btnCloseSidebarMobile');
    const sidebar = document.querySelector('.app-sidebar');
    const backdrop = document.getElementById('sidebarBackdrop');

    if (!sidebar) return;

    function closeMobileSidebar() {
        sidebar.classList.remove('show');
        if (backdrop) backdrop.classList.remove('show');
        document.body.style.overflow = '';
    }

    function openMobileSidebar() {
        sidebar.classList.add('show');
        if (backdrop) backdrop.classList.add('show');
        document.body.style.overflow = 'hidden';
    }

    // Khôi phục trạng thái thu nhỏ sidebar desktop đã lưu
    if (window.innerWidth >= 992) {
        const isCollapsed = localStorage.getItem('library_sidebar_collapsed') === 'true';
        if (isCollapsed) {
            document.body.classList.add('sidebar-collapsed');
        }
    }

    if (toggleBtn) {
        toggleBtn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            if (window.innerWidth < 992) {
                if (sidebar.classList.contains('show')) {
                    closeMobileSidebar();
                } else {
                    openMobileSidebar();
                }
            } else {
                document.body.classList.toggle('sidebar-collapsed');
                const isCollapsed = document.body.classList.contains('sidebar-collapsed');
                localStorage.setItem('library_sidebar_collapsed', isCollapsed);
            }
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', function (e) {
            e.preventDefault();
            closeMobileSidebar();
        });
    }

    if (backdrop) {
        backdrop.addEventListener('click', function () {
            closeMobileSidebar();
        });
    }

    // Tự động đóng sidebar mobile khi nhấn vào bất kỳ link điều hướng nào
    const menuLinks = sidebar.querySelectorAll('.menu-link');
    menuLinks.forEach(link => {
        link.addEventListener('click', function () {
            if (window.innerWidth < 992) {
                closeMobileSidebar();
            }
        });
    });

    window.addEventListener('resize', function () {
        if (window.innerWidth >= 992) {
            closeMobileSidebar();
        }
    });
}

/* ==========================================================================
   3. CHUYỂN ĐỔI CHẾ ĐỘ XEM SÁCH: THẺ LƯỚI (CARD) & BẢNG (TABLE)
   ========================================================================== */
function initViewModeToggle() {
    const btnGrid = document.getElementById('btnViewGrid');
    const btnTable = document.getElementById('btnViewTable');
    const gridContainer = document.getElementById('bookGridView');
    const tableContainer = document.getElementById('bookTableView');

    if (!btnGrid || !btnTable || !gridContainer || !tableContainer) return;

    // Đọc chế độ xem đã lưu (Mặc định là Card Grid)
    const savedView = localStorage.getItem('library_view_mode') || 'grid';
    setView(savedView);

    btnGrid.addEventListener('click', () => setView('grid'));
    btnTable.addEventListener('click', () => setView('table'));

    function setView(mode) {
        localStorage.setItem('library_view_mode', mode);
        if (mode === 'grid') {
            gridContainer.classList.remove('d-none');
            tableContainer.classList.add('d-none');
            btnGrid.classList.add('active', 'btn-primary');
            btnGrid.classList.remove('btn-outline-secondary');
            btnTable.classList.remove('active', 'btn-primary');
            btnTable.classList.add('btn-outline-secondary');
        } else {
            gridContainer.classList.add('d-none');
            tableContainer.classList.remove('d-none');
            btnTable.classList.add('active', 'btn-primary');
            btnTable.classList.remove('btn-outline-secondary');
            btnGrid.classList.remove('active', 'btn-primary');
            btnGrid.classList.add('btn-outline-secondary');
        }
    }
}

/* ==========================================================================
   4. MODAL XÁC NHẬN XÓA DÙNG CHUNG (SAFE CONFIRM MODAL)
   ========================================================================== */
function initDeleteConfirmModal() {
    const deleteModalEl = document.getElementById('deleteConfirmModal');
    if (!deleteModalEl) return;

    const deleteForm = document.getElementById('deleteConfirmForm');
    const itemNameSpan = document.getElementById('deleteItemName');

    document.querySelectorAll('.btn-delete-confirm').forEach(button => {
        button.addEventListener('click', function (e) {
            e.preventDefault();
            const actionUrl = this.getAttribute('data-action');
            const itemName = this.getAttribute('data-item-name') || 'mục này';

            if (deleteForm) deleteForm.setAttribute('action', actionUrl);
            if (itemNameSpan) itemNameSpan.textContent = `"${itemName}"`;

            const modalInstance = bootstrap.Modal.getOrCreateInstance(deleteModalEl);
            modalInstance.show();
        });
    });
}

/* ==========================================================================
   5. TỰ ĐỘNG ĐÓNG THÔNG BÁO FLASH MESSAGE SAU 4.5 GIÂY
   ========================================================================== */
function initAutoHideAlerts() {
    const alerts = document.querySelectorAll('.auto-dismiss-alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 4500);
    });
}

/* ==========================================================================
   6. BÀN PHÍM PHÍM TẮT COMMAND BAR (CTRL + K / CMD + K)
   ========================================================================== */
function initKeyboardShortcuts() {
    window.addEventListener('keydown', function (e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            const searchInput = document.querySelector('.header-search input');
            if (searchInput) {
                e.preventDefault();
                searchInput.focus();
                searchInput.select();
            }
        }
        if (e.key === 'Escape') {
            const searchInput = document.querySelector('.header-search input');
            if (searchInput && document.activeElement === searchInput) {
                searchInput.blur();
            }
        }
    });
}

/* ==========================================================================
   SIDEBAR ACTIVE STATE HIGHLIGHT (CLIENT-SIDE BACKUP)
   ========================================================================== */
function initSidebarActiveState() {
    const path = window.location.pathname;
    const sidebar = document.querySelector('.app-sidebar');
    if (!sidebar) return;

    if (path.indexOf('/reports') !== -1) {
        sidebar.querySelectorAll('.menu-item.active').forEach(item => item.classList.remove('active'));
        const reportLink = sidebar.querySelector('a[href*="/reports"]');
        if (reportLink && reportLink.parentElement) {
            reportLink.parentElement.classList.add('active');
        }
    }
}
