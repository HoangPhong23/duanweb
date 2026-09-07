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

    // Sử dụng config object để truyền params chuẩn xác như các trang gọi
    const endpoints = [
        { url: '/api/roles', params: { active: true } }, // UsersPage, RolesPage, AssignRoleModal
        { url: '/api/centers/lite' },                    // UsersPage, ClassesPage dropdown
        { url: '/api/user-views' },                      // UsersPage (default no filter)
        { url: '/api/user-stats/roles' },                // UsersPage role stats
        { url: '/api/classes' },                         // ClassesPage
        { url: '/api/centers' },                         // CentersPage
        { url: '/api/students/warnings' },               // Dashboard warnings
        { url: '/api/students' },                        // StudentsPage
        { url: '/api/dashboard/summary' },               // Dashboard
        { url: '/api/permissions/groups' },              // RolesPage
        { url: '/api/programs' },                        // StudentProfilePage
        { url: '/api/programs/lite' },                   // ClassesPage
    ];

    for (const ep of endpoints) {
        api.get(ep.url, { params: ep.params }).catch(() => {});
    }
}
