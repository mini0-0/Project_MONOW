package com.monow.domain.dart.entity;

import com.monow.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "dart_disclosures")
@Getter
public class DartDisclosure extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rcept_no", length = 14, nullable = false, unique = true)
    private String receiptNumber;

    @Column(name = "corp_code", length = 8, nullable = false)
    private String corpCode;

    @Column(name = "report_nm", nullable = false, columnDefinition = "text")
    private String reportName;

    @Column(name = "rcept_dt", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "flr_nm", columnDefinition = "text")
    private String filerName;

    protected DartDisclosure() {
    }
}
