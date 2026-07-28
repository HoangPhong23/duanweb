/**
 * Program categories mapping
 * Maps categoryCode (backend) to display label (frontend)
 */
export const PROGRAM_CATEGORIES = {
    IELTS: 'Lớp IELTS',
    TOEIC: 'Lớp TOEIC',
    ENGLISH_BASIC: 'Lớp Tiếng Anh Cơ Bản',
    OTHER: 'Khác',
} as const;

/**
 * Get category label by code
 */
export const getCategoryLabel = (categoryCode: string): string => {
    return PROGRAM_CATEGORIES[categoryCode as keyof typeof PROGRAM_CATEGORIES] || categoryCode;
};

/**
 * Get all category options for dropdown
 */
export const getCategoryOptions = () => {
    return Object.entries(PROGRAM_CATEGORIES).map(([code, label]) => ({
        value: code,
        label,
    }));
};

/**
 * Main categories for program creation form
 */
export const MAIN_CATEGORIES = [
    { value: 'IELTS', label: 'Lớp IELTS' },
    { value: 'TOEIC', label: 'Lớp TOEIC' },
    { value: 'ENGLISH_BASIC', label: 'Lớp Tiếng Anh Cơ Bản' },
] as const;
