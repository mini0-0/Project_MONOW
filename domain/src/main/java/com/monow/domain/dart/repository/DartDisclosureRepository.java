package com.monow.domain.dart.repository;

import com.monow.domain.dart.entity.DartDisclosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DartDisclosureRepository extends JpaRepository<DartDisclosure, Long> {

    Optional<DartDisclosure> findByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumber(String receiptNumber);

    @Modifying
    @Query(value = """
        INSERT INTO dart_disclosures (
            rcept_no, corp_code, report_nm, rcept_dt, flr_nm,
            created_at, updated_at
        )
        VALUES (
            :receiptNumber, :corpCode, :reportName, :receiptDate, :filerName,
            CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul',
            CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'
        )
        ON CONFLICT (rcept_no) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("receiptNumber") String receiptNumber,
            @Param("corpCode") String corpCode,
            @Param("reportName") String reportName,
            @Param("receiptDate") LocalDate receiptDate,
            @Param("filerName") String filerName
    );

}
