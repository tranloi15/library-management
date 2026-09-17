/**
 * HỆ THỐNG QUẢN LÝ THƯ VIỆN - CLIENT SCRIPTS
 * Phụ trách: Hưng (Khung Giao diện Master Layout & UX)
 */

document.addEventListener('DOMContentLoaded', function () {
    initThemeToggle();
    initViewModeToggle();
    initDeleteConfirmModal();
    initAutoHideAlerts();
    initMobileSidebar();
    initKeyboardShortcuts();

    // Dọn dẹp trạng thái bảng màu cũ nếu có, luôn giữ mặc định Sapphire Tech
    localStorage.removeItem('library_palette');
    localStorage.removeItem('ptit_library_palette');
    document.documentElement.removeAttribute('data-theme-palette');
    document.documentElement.removeAttribute('data-theme-style');
});

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
   3. MODAL XÁC NHẬN XÓA DÙNG CHUNG (SAFE CONFIRM MODAL)
   Ngăn ngừa việc vô tình bấm xóa tài liệu hoặc độc giả
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
   4. TỰ ĐỘNG ĐÓNG THÔNG BÁO FLASH MESSAGE SAU 4.5 GIÂY
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
   5. MOBILE SIDEBAR TOGGLE
   ========================================================================== */
function initMobileSidebar() {
    const toggleBtn = document.getElementById('btnToggleSidebar');
    const sidebar = document.querySelector('.app-sidebar');
    const backdrop = document.getElementById('sidebarBackdrop');

    if (!toggleBtn || !sidebar) return;

    toggleBtn.addEventListener('click', function () {
        sidebar.classList.toggle('show');
        if (backdrop) backdrop.classList.toggle('show');
    });

    if (backdrop) {
        backdrop.addEventListener('click', function () {
            sidebar.classList.remove('show');
            backdrop.classList.remove('show');
        });
    }
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
