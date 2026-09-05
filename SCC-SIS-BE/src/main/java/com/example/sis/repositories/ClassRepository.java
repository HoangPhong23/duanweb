package com.example.sis.repositories;

import com.example.sis.models.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Integer> {

    /**
     * Lấy danh sách lớp học theo trung tâm và chưa bị soft delete
     * Sắp xếp theo ngày bắt đầu và ID (cho pagination seek-friendly)
     * JOIN FETCH center, program, createdBy để tránh N+1 Query
     */
    @Query("SELECT c FROM ClassEntity c " +
            "JOIN FETCH c.center JOIN FETCH c.program " +
            "LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.updatedBy " +
            "WHERE c.center.centerId = :centerId AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findByCenterIdOrderByStartDateDesc(@Param("centerId") Integer centerId);

    /**
     * Lấy tất cả lớp học chưa bị soft delete (cho Super Admin)
     * Sắp xếp theo ngày bắt đầu và ID
     * JOIN FETCH center, program, createdBy để tránh N+1 Query
     */
    @Query("SELECT c FROM ClassEntity c " +
            "JOIN FETCH c.center JOIN FETCH c.program " +
            "LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.updatedBy " +
            "WHERE c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findAllOrderByStartDateDesc();

    /**
     * Lấy lớp học theo ID và chưa bị soft delete
     * JOIN FETCH center, program, createdBy để tránh N+1 Query
     */
    @Query("SELECT c FROM ClassEntity c " +
            "JOIN FETCH c.center JOIN FETCH c.program " +
            "LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.updatedBy " +
            "WHERE c.classId = :classId AND c.deletedAt IS NULL")
    Optional<ClassEntity> findByIdAndNotDeleted(@Param("classId") Integer classId);

    /**
     * Lấy danh sách lớp học theo chương trình học
     * JOIN FETCH center, program, createdBy để tránh N+1 Query
     */
    @Query("SELECT c FROM ClassEntity c " +
            "JOIN FETCH c.center JOIN FETCH c.program " +
            "LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.updatedBy " +
            "WHERE c.program.programId = :programId AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findByProgramIdOrderByStartDateDesc(@Param("programId") Integer programId);

    /**
     * Lấy danh sách lớp học theo trạng thái
     * JOIN FETCH center, program, createdBy để tránh N+1 Query
     */
    @Query("SELECT c FROM ClassEntity c " +
            "JOIN FETCH c.center JOIN FETCH c.program " +
            "LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.updatedBy " +
            "WHERE c.status = :status AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findByStatusOrderByStartDateDesc(@Param("status") ClassEntity.ClassStatus status);

    /**
     * Lấy danh sách lớp học theo trung tâm và trạng thái
     * JOIN FETCH center, program, createdBy để tránh N+1 Query
     */
    @Query("SELECT c FROM ClassEntity c " +
            "JOIN FETCH c.center JOIN FETCH c.program " +
            "LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.updatedBy " +
            "WHERE c.center.centerId = :centerId AND c.status = :status AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findByCenterIdAndStatusOrderByStartDateDesc(
            @Param("centerId") Integer centerId,
            @Param("status") ClassEntity.ClassStatus status);

    /**
     * Kiểm tra tên lớp đã tồn tại trong trung tâm chưa (cho unique constraint)
     */
    @Query("SELECT COUNT(c) > 0 FROM ClassEntity c " +
            "WHERE c.center.centerId = :centerId AND c.name = :name AND c.deletedAt IS NULL")
    boolean existsByCenterIdAndName(@Param("centerId") Integer centerId, @Param("name") String name);

    /**
     * Kiểm tra tên lớp đã tồn tại trong trung tâm chưa (trừ lớp hiện tại - dùng cho
     * update)
     */
    @Query("SELECT COUNT(c) > 0 FROM ClassEntity c " +
            "WHERE c.center.centerId = :centerId AND c.name = :name " +
            "AND c.classId != :excludeClassId AND c.deletedAt IS NULL")
    boolean existsByCenterIdAndNameExcludingId(
            @Param("centerId") Integer centerId,
            @Param("name") String name,
            @Param("excludeClassId") Integer excludeClassId);

    /**
     * Lấy centerId của lớp (phục vụ kiểm quyền theo trung tâm)
     */
    @Query("SELECT c.center.centerId FROM ClassEntity c WHERE c.classId = :classId")
    Integer findCenterIdByClassId(@Param("classId") Integer classId);

    /**
     * Kiểm tra lớp có được phép ghi danh học viên (chưa kết thúc, chưa bị huỷ)
     */
    @Query("""
        SELECT CASE WHEN (c.deletedAt IS NULL
                       AND c.status <> com.example.sis.models.ClassEntity.ClassStatus.CANCELLED
                       AND c.status <> com.example.sis.models.ClassEntity.ClassStatus.FINISHED)
                    THEN true ELSE false END
        FROM ClassEntity c
        WHERE c.classId = :classId
        """)
    Boolean isEnrollable(@Param("classId") Integer classId);

    /**
     * Lấy danh sách lớp học mà giảng viên được phân công
     * Lọc các assignment đang hiệu lực (end_date IS NULL hoặc end_date >= ngày hiện tại)
     * JOIN FETCH center, program, createdBy để tránh N+1 Query
     */
    @Query("SELECT DISTINCT c FROM ClassEntity c " +
            "JOIN FETCH c.center JOIN FETCH c.program " +
            "LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.updatedBy " +
            "JOIN c.classTeachers ct " +
            "WHERE ct.teacher.userId = :lecturerId " +
            "AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE) " +
            "AND c.deletedAt IS NULL " +
            "ORDER BY c.startDate DESC, c.classId DESC")
    List<ClassEntity> findClassesByLecturerId(@Param("lecturerId") Integer lecturerId);

    /**
     * Kiểm tra giảng viên có được phân công vào lớp không (và còn hiệu lực)
     */
    @Query("SELECT COUNT(ct) > 0 FROM ClassTeacher ct " +
            "WHERE ct.teacher.userId = :lecturerId " +
            "AND ct.classEntity.classId = :classId " +
            "AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE)")
    boolean isLecturerAssignedToClass(@Param("lecturerId") Integer lecturerId, 
                                      @Param("classId") Integer classId);

}