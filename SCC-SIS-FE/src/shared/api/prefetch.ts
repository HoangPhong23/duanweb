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
export function prefetchCommonData(userRoles: string[] = []) {
    if (prefetched) return;
    prefetched = true;

    const isAdmin = userRoles.includes('SUPER_ADMIN');
    const isManager = userRoles.includes('CENTER_MANAGER');
    const isStaff = userRoles.includes('ACADEMIC_STAFF');
    const isLecturer = userRoles.includes('LECTURER') || userRoles.includes('TEACHER');
    
    const isStaffOrAdmin = isAdmin || isManager || isStaff;

    // Wave 1: Các API cần cho Dashboard & Global UI (TopNav, Menu)
    const wave1 = [];
    if (isStaffOrAdmin || isLecturer) {
        wave1.push({ url: '/api/v1/dashboard/summary' });
        wave1.push({ url: '/api/students/warnings' });
    }
    if (isStaffOrAdmin) {
        wave1.push({ url: '/api/centers/lite' });
    }

    // Wave 2: Các API của các trang con (Users, Classes, Students, Roles) - Delay 1 giây
    const wave2 = [];
    
    if (isAdmin) {
        wave2.push({ url: '/api/roles', params: { active: true } });
        wave2.push({ url: '/api/permissions/groups' });
        wave2.push({ url: '/api/centers/all' });
    }
    
    if (isAdmin || isManager) {
        wave2.push({ url: '/api/user-views' });
        wave2.push({ url: '/api/user-stats/roles' });
    }
    
    if (isStaffOrAdmin) {
        wave2.push({ url: '/api/classes' });
        wave2.push({ url: '/api/students' });
        wave2.push({ url: '/api/programs' });
        wave2.push({ url: '/api/programs/lite' });
    }

    // Chạy Wave 1 ngay lập tức
    for (const ep of wave1) {
        api.get(ep.url, { params: ep.params }).catch(() => {});
    }

    // Chạy Wave 2 sau 1000ms để nhường băng thông cho UI Dashboard render trước
    if (wave2.length > 0) {
        setTimeout(() => {
            for (const ep of wave2) {
                api.get(ep.url, { params: ep.params }).catch(() => {});
            }
        }, 1000);
    }
}
