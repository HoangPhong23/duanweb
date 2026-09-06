// src/shared/api/prefetch.ts
// Prefetch dữ liệu chung ngay sau khi đăng nhập để khi user click vào bất kỳ trang nào,
// dữ liệu đã có sẵn trong cache RAM frontend (0ms thay vì 3-4s qua mạng).
import api from './http';

let prefetched = false;

/**
 * Gọi song song tất cả API mà các trang hay dùng.
 * Kết quả sẽ tự động được lưu vào cache trong http.ts interceptor.
 * Chỉ chạy 1 lần duy nhất sau khi đăng nhập.
 */
export function prefetchCommonData() {
    if (prefetched) return;
    prefetched = true;

    // Fire-and-forget: không await, không block UI
    const endpoints = [
        '/api/roles',                    // UsersPage, AssignRoleModal
        '/api/centers/lite',             // UsersPage, ClassesPage dropdown
        '/api/user-views',               // UsersPage (default no filter)
        '/api/user-stats/roles',         // UsersPage role stats
        '/api/classes',                  // ClassesPage
        '/api/centers',                  // CentersPage
        '/api/students/warnings',        // Dashboard warnings
        '/api/students/with-enrollments', // StudentsPage
        '/api/dashboard/summary',        // Dashboard
    ];

    for (const url of endpoints) {
        api.get(url).catch(() => {
            // Bỏ qua lỗi 403/404 - user có thể không có quyền truy cập một số API
        });
    }
}
