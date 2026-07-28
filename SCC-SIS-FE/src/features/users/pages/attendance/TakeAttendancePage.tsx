import { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Save, CheckCircle, XCircle, RefreshCw, Copy, Check } from 'lucide-react';
import { useToast } from '@/shared/hooks/useToast';
import { useUserProfile } from '@/stores/userProfile';
import {
    createAttendanceSession,
    getAttendanceSessionDetail,
    updateAttendanceSession,
    setAttendanceCode,
    type AttendanceStatus as ApiAttendanceStatus,
} from '@/shared/api/attendance';
import http from '@/shared/api/http';

type AttendanceStatus = 'PRESENT' | 'ABSENT' | null;

type Student = {
    id: number;
    enrollmentId: number;
    studentCode: string;
    fullName: string;
    email: string;
};

type AttendanceRecord = {
    studentId: number;
    enrollmentId?: number;
    recordId?: number;
    status: AttendanceStatus;
    note: string;
};

// Helper to split full name into "Họ đệm" and "Tên"
function splitFullName(name: string) {
    if (!name) return { lastName: '', firstName: '' };
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return { lastName: '', firstName: parts[0] };
    const firstName = parts.pop() || '';
    const lastName = parts.join(' ');
    return { lastName, firstName };
}

export default function TakeAttendancePage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { me } = useUserProfile();
    const { success: showSuccessToast, error: showErrorToast } = useToast();

    const classId = useMemo(() => searchParams.get('classId') || '1', [searchParams]);
    const className = useMemo(() => searchParams.get('className') || 'Cấu trúc dữ liệu và giải thuật', [searchParams]);
    const sessionIdParam = useMemo(() => searchParams.get('sessionId'), [searchParams]);
    const viewMode = useMemo(() => searchParams.get('viewMode') || 'month', [searchParams]);
    const date = useMemo(() => {
        const urlDate = searchParams.get('date');
        if (!urlDate) {
            const today = new Date();
            const year = today.getFullYear();
            const month = String(today.getMonth() + 1).padStart(2, '0');
            const day = String(today.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        }
        return urlDate;
    }, [searchParams]);

    const [students, setStudents] = useState<Student[]>([]);
    const [attendanceRecords, setAttendanceRecords] = useState<Map<number, AttendanceRecord>>(new Map());
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [isEditMode, setIsEditMode] = useState(false);
    const [sessionId, setSessionId] = useState<number | null>(null);

    // Từ khóa điểm danh
    const [customCode, setCustomCode] = useState('');
    const [codeActive, setCodeActive] = useState(false);
    const [codeSessionId, setCodeSessionId] = useState<number | null>(null);
    const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);

    // Tải dữ liệu
    useEffect(() => {
        const fetchData = async () => {
            try {
                setIsLoading(true);

                if (sessionIdParam) {
                    setIsEditMode(true);
                    setSessionId(parseInt(sessionIdParam));

                    const sessionResponse = await getAttendanceSessionDetail(parseInt(sessionIdParam));
                    const sessionData = sessionResponse.data;

                    const studentsData: Student[] = sessionData.records.map((record: any) => ({
                        id: record.studentId,
                        enrollmentId: record.enrollmentId,
                        studentCode: record.studentCode || `SV${record.studentId}`,
                        fullName: record.studentName,
                        email: record.studentEmail || '',
                    }));
                    setStudents(studentsData);

                    const existingRecords = new Map(
                        sessionData.records.map((r: any) => [
                            r.enrollmentId,
                            {
                                studentId: r.studentId,
                                enrollmentId: r.enrollmentId,
                                recordId: r.recordId,
                                status: r.status as AttendanceStatus,
                                note: r.notes || '',
                            },
                        ])
                    );
                    setAttendanceRecords(existingRecords);

                    if ((sessionData as any).attendanceCode) {
                        setCustomCode((sessionData as any).attendanceCode);
                        setCodeActive((sessionData as any).codeEnabled ?? false);
                        setCodeSessionId(parseInt(sessionIdParam));
                    }
                } else {
                    const response = await http.get(`/api/classes/${classId}/students`, {
                        params: { status: 'ACTIVE', page: 0, size: 1000 },
                    });
                    const enrollments = response.data.content || response.data.items || response.data;

                    const studentsData: Student[] = enrollments.map((enrollment: any) => ({
                        id: enrollment.studentId,
                        enrollmentId: enrollment.enrollmentId,
                        studentCode: enrollment.studentCode || `SV${enrollment.studentId}`,
                        fullName: enrollment.studentName,
                        email: enrollment.studentEmail,
                    }));
                    setStudents(studentsData);

                    const initialRecords = new Map(
                        studentsData.map((s) => [
                            s.enrollmentId,
                            {
                                studentId: s.id,
                                enrollmentId: s.enrollmentId,
                                status: 'PRESENT' as AttendanceStatus,
                                note: '',
                            },
                        ])
                    );
                    setAttendanceRecords(initialRecords);
                }
            } catch (error) {
                showErrorToast('Lỗi', 'Không thể tải dữ liệu');
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, [classId, sessionIdParam]);

    // Polling refresh tự động khi đang mở mã
    useEffect(() => {
        if (codeActive && codeSessionId) {
            pollingRef.current = setInterval(async () => {
                try {
                    const res = await getAttendanceSessionDetail(codeSessionId);
                    const updatedRecords = new Map<number, AttendanceRecord>(attendanceRecords);
                    res.data.records.forEach((r: any) => {
                        const existing = updatedRecords.get(r.enrollmentId);
                        if (existing) {
                            updatedRecords.set(r.enrollmentId, {
                                ...existing,
                                status: r.status as AttendanceStatus,
                                recordId: r.recordId,
                            });
                        }
                    });
                    setAttendanceRecords(new Map(updatedRecords));
                } catch (_) {}
            }, 4000);
        } else if (pollingRef.current) {
            clearInterval(pollingRef.current);
            pollingRef.current = null;
        }
        return () => {
            if (pollingRef.current) clearInterval(pollingRef.current);
        };
    }, [codeActive, codeSessionId]);

    // Handlers
    const handleStatusChange = (enrollmentId: number, status: AttendanceStatus) => {
        setAttendanceRecords((prev) => {
            const newMap = new Map(prev);
            const record = newMap.get(enrollmentId);
            if (record) newMap.set(enrollmentId, { ...record, status });
            return newMap;
        });
    };

    const handleNoteChange = (enrollmentId: number, note: string) => {
        setAttendanceRecords((prev) => {
            const newMap = new Map(prev);
            const record = newMap.get(enrollmentId);
            if (record) newMap.set(enrollmentId, { ...record, note });
            return newMap;
        });
    };

    const ensureSessionCreated = async (): Promise<number> => {
        if (isEditMode && sessionId) return sessionId;
        const records = students.map((student) => ({
            enrollmentId: student.enrollmentId!,
            studentId: student.id,
            status: 'ABSENT' as ApiAttendanceStatus,
            notes: undefined,
        }));

        const res = await createAttendanceSession({
            classId: parseInt(classId),
            teacherId: me!.userId,
            attendanceDate: date,
            notes: '',
            records,
        });
        const newSessionId = (res.data as any).sessionId;
        setSessionId(newSessionId);
        setIsEditMode(true);
        return newSessionId;
    };

    // Nút "Lưu": Lưu cả Từ khóa điểm danh + Kết quả điểm danh
    const handleSave = async () => {
        if (!me?.userId && !isEditMode) {
            showErrorToast('Lỗi', 'Không tìm thấy thông tin giảng viên');
            return;
        }

        setIsSubmitting(true);
        try {
            let currentSid = sessionId;
            if (!isEditMode || !currentSid) {
                currentSid = await ensureSessionCreated();
            }

            // 1. Lưu từ khóa điểm danh nếu có nhập
            if (customCode.trim()) {
                await setAttendanceCode(currentSid, {
                    attendanceCode: customCode.trim().toUpperCase(),
                    codeEnabled: true,
                });
                setCodeActive(true);
                setCodeSessionId(currentSid);
            } else if (codeActive && currentSid) {
                // Tắt mã nếu xóa trống
                await setAttendanceCode(currentSid, {
                    attendanceCode: '',
                    codeEnabled: false,
                });
                setCodeActive(false);
            }

            // 2. Lưu bảng điểm danh
            const records = students.map((student) => {
                const record = attendanceRecords.get(student.enrollmentId);
                return {
                    recordId: record?.recordId!,
                    enrollmentId: student.enrollmentId!,
                    studentId: student.id,
                    status: record?.status || 'ABSENT',
                    notes: record?.note || undefined,
                };
            });

            if (isEditMode && sessionId) {
                await updateAttendanceSession(sessionId, { notes: '', records });
            } else {
                await createAttendanceSession({
                    classId: parseInt(classId),
                    teacherId: me!.userId,
                    attendanceDate: date,
                    notes: '',
                    records,
                });
            }

            showSuccessToast('Thành công', 'Đã lưu điểm danh và từ khóa thành công');
            setTimeout(() => navigate(`/attendance?view=${viewMode}&date=${date}`), 400);
        } catch (error: any) {
            const errorData = error?.response?.data;
            const errorMessage = errorData?.message || errorData?.error || 'Không thể lưu điểm danh';
            showErrorToast('Lỗi', errorMessage);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleGoBack = () => navigate(`/attendance?view=${viewMode}&date=${date}`);

    // Thống kê
    const totalCount = students.length;
    const presentCount = Array.from(attendanceRecords.values()).filter((r) => r.status === 'PRESENT').length;
    const absentCount = Array.from(attendanceRecords.values()).filter((r) => r.status === 'ABSENT').length;
    const absentPercentage = totalCount > 0 ? Math.round((absentCount / totalCount) * 100) : 0;

    const formattedDateStr = useMemo(() => {
        const [y, m, d] = date.split('-');
        return `${d}/${m}/${y}`;
    }, [date]);

    return (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs z-50 flex items-center justify-center p-4 overflow-y-auto">
            <motion.div
                initial={{ opacity: 0, scale: 0.96 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.96 }}
                className="bg-white rounded-xl shadow-2xl w-full max-w-5xl my-auto overflow-hidden border border-gray-200"
            >
                {/* 1. Header (Tương tự hình mẫu) */}
                <div className="flex items-start justify-between px-6 py-4 border-b border-gray-200 bg-white">
                    <div>
                        <div className="flex items-center gap-4 flex-wrap">
                            <h2 className="text-base font-bold text-gray-900">
                                Học phần: {className} - 07:00 - 09:40 (Tiết 1-3)
                            </h2>
                            <span className="text-sm font-semibold text-red-600">
                                Vắng mặt({absentCount})
                            </span>
                        </div>
                        <div className="text-xs font-medium text-gray-600 mt-1">
                            Vắng mặt(Số buổi/Số tiết/Tỷ lệ):{' '}
                            <span className="font-bold text-gray-900">
                                ({absentCount}/{totalCount * 3 || 9}/{absentPercentage}%)
                            </span>
                        </div>
                    </div>
                    <button
                        onClick={handleGoBack}
                        className="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-md hover:bg-gray-100"
                        title="Đóng"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* 2. Sub-header: Tên lớp, Phòng học, Từ khóa điểm danh & Nút Lưu */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 px-6 py-3 bg-gray-50/70 border-b border-gray-200">
                    <div>
                        <h3 className="text-sm font-bold text-gray-900">
                            Lớp: {className}-2-2-25(N01).LT/LT+TH
                        </h3>
                        <p className="text-xs text-gray-500 font-medium mt-0.5">
                            &#123;Phòng học: VPC2-12A01&#125;
                        </p>
                    </div>

                    {/* Từ khóa điểm danh + Nút Lưu */}
                    <div className="flex items-center gap-2">
                        <span className="text-xs font-medium text-gray-700 whitespace-nowrap">
                            Từ khóa điểm danh
                        </span>
                        <input
                            type="text"
                            placeholder="Từ khóa điểm danh"
                            value={customCode}
                            onChange={(e) => setCustomCode(e.target.value.toUpperCase())}
                            className="w-44 px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none bg-white uppercase font-semibold text-gray-800"
                        />
                        <button
                            onClick={handleSave}
                            disabled={isSubmitting}
                            className="bg-[#1a73e8] hover:bg-[#1557b0] text-white px-4 py-1.5 rounded-md text-sm font-semibold flex items-center gap-1.5 shadow-sm transition-all disabled:opacity-50"
                        >
                            <Save size={16} />
                            Lưu
                        </button>
                    </div>
                </div>

                {/* Status indicator banner when code active */}
                {codeActive && (
                    <div className="bg-indigo-50 border-b border-indigo-100 px-6 py-2 flex items-center justify-between text-xs text-indigo-800">
                        <div className="flex items-center gap-2">
                            <RefreshCw size={12} className="animate-spin text-indigo-600" />
                            <span>Mã điểm danh <strong>{customCode}</strong> đang mở — Học viên nhập mã để tự điểm danh. Danh sách sẽ tự cập nhật.</span>
                        </div>
                        <button
                            onClick={async () => {
                                if (sessionId) {
                                    await setAttendanceCode(sessionId, { attendanceCode: '', codeEnabled: false });
                                    setCodeActive(false);
                                    showSuccessToast('Thông báo', 'Đã tắt mã điểm danh');
                                }
                            }}
                            className="text-red-600 hover:underline font-semibold"
                        >
                            Tắt mã
                        </button>
                    </div>
                )}

                {/* 3. Bảng danh sách sinh viên (Giao diện chuẩn hình ảnh) */}
                <div className="max-h-[60vh] overflow-y-auto">
                    <table className="w-full text-left border-collapse">
                        <thead className="bg-[#f8f9fa] border-b border-gray-200 sticky top-0 z-10">
                            <tr>
                                <th className="px-4 py-2.5 text-xs font-bold text-gray-700 w-14 text-center">STT</th>
                                <th className="px-4 py-2.5 text-xs font-bold text-gray-700 w-36">Mã số</th>
                                <th className="px-4 py-2.5 text-xs font-bold text-gray-700">Họ đệm</th>
                                <th className="px-4 py-2.5 text-xs font-bold text-gray-700 w-28">Tên</th>
                                <th className="px-4 py-2.5 text-xs font-bold text-gray-700 text-center w-28">Có mặt</th>
                                <th className="px-4 py-2.5 text-xs font-bold text-gray-700 text-center w-20">Vắng</th>
                                <th className="px-4 py-2.5 text-xs font-bold text-gray-700 w-44">Ghi chú</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 text-xs text-gray-800">
                            {isLoading ? (
                                <tr>
                                    <td colSpan={7} className="px-4 py-8 text-center text-sm text-gray-500">
                                        Đang tải danh sách học viên...
                                    </td>
                                </tr>
                            ) : students.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="px-4 py-8 text-center text-sm text-gray-500">
                                        Không có học viên nào trong lớp
                                    </td>
                                </tr>
                            ) : (
                                students.map((student, index) => {
                                    const record = attendanceRecords.get(student.enrollmentId);
                                    const { lastName, firstName } = splitFullName(student.fullName);
                                    const isPresent = record?.status === 'PRESENT';
                                    const isAbsent = record?.status === 'ABSENT';

                                    return (
                                        <tr
                                            key={student.enrollmentId}
                                            className={`hover:bg-blue-50/50 transition-colors ${
                                                index % 2 === 1 ? 'bg-gray-50/40' : 'bg-white'
                                            }`}
                                        >
                                            <td className="px-4 py-2.5 text-center font-medium text-gray-600">
                                                {index + 1}
                                            </td>
                                            <td className="px-4 py-2.5 font-semibold text-gray-900 uppercase">
                                                {student.studentCode}
                                            </td>
                                            <td className="px-4 py-2.5 uppercase font-medium text-gray-700">
                                                {lastName}
                                            </td>
                                            <td className="px-4 py-2.5 uppercase font-semibold text-gray-900">
                                                {firstName}
                                            </td>
                                            <td className="px-4 py-2.5 text-center">
                                                <input
                                                    type="checkbox"
                                                    checked={isPresent}
                                                    onChange={() => handleStatusChange(student.enrollmentId, 'PRESENT')}
                                                    className="w-4 h-4 rounded border-gray-300 text-green-600 focus:ring-green-500 cursor-pointer"
                                                />
                                            </td>
                                            <td className="px-4 py-2.5 text-center">
                                                <input
                                                    type="checkbox"
                                                    checked={isAbsent}
                                                    onChange={() => handleStatusChange(student.enrollmentId, 'ABSENT')}
                                                    className="w-4 h-4 rounded border-gray-300 text-red-600 focus:ring-red-500 cursor-pointer"
                                                />
                                            </td>
                                            <td className="px-4 py-2.5">
                                                <input
                                                    type="text"
                                                    value={record?.note || ''}
                                                    onChange={(e) => handleNoteChange(student.enrollmentId, e.target.value)}
                                                    placeholder="Ghi chú..."
                                                    className="w-full px-2 py-1 text-xs border border-gray-200 rounded focus:border-blue-500 outline-none"
                                                />
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </motion.div>
        </div>
    );
}
