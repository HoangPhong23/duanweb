// src/shared/api/student-warnings.ts
import http from './http';
import { getAllStudentsWithEnrollments } from './students';
import { getStudentAttendanceHistory } from './attendance';
import { getStudentGradesByStudentId } from './grade-entries';
import { listClasses } from './classes';

export interface StudentWarning {
    studentId: number;
    code: string;
    name: string;
    reason: string;
    detail: string;
    program: string;
    classCode: string;
    severity: 'HIGH' | 'MEDIUM' | 'LOW';
}

export interface StudentWarningsResponse {
    warnings: StudentWarning[];
    totalCount: number;
}

export interface MyWarning {
    classId: number;
    className: string;
    programName: string;
    absentCount: number;
    failCount: number;
    hasAbsenceWarning: boolean;
    hasFailWarning: boolean;
}

export const studentWarningsApi = {
    /**
     * GET /api/students/my-warnings
     * Lấy cảnh báo của học viên hiện tại (vắng > 2, trượt > 2)
     */
    getMyWarnings: async (): Promise<MyWarning[]> => {
        const response = await http.get<MyWarning[]>('/api/students/my-warnings');
        return response.data;
    },

    /**
     * GET /api/students/warnings?centerId={centerId}
     * Lấy danh sách học sinh bị cảnh báo theo trung tâm
     * Tổng hợp từ attendance (vắng mặt) và grades (thi trượt)
     */
    getStudentWarnings: async (centerId?: number): Promise<StudentWarningsResponse> => {
        const params = centerId ? { centerId } : {};
        const response = await http.get<StudentWarningsResponse>('/api/students/warnings', { params });
        return response.data;
    },
};
