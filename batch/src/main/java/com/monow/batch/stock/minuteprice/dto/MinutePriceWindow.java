package com.monow.batch.stock.minuteprice.dto;

import org.springframework.batch.core.JobParameters;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record MinutePriceWindow(LocalDate tradeDate, LocalTime startTime, LocalTime endTime) {
    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public static MinutePriceWindow from(JobParameters parameters, Clock clock) {
        if (!"UN".equals(parameters.getString("marketCode"))
                || !"INTEGRATED_DAY".equals(parameters.getString("session"))) {
            throw new IllegalArgumentException("분봉 수집은 통합 시장의 INTEGRATED_DAY 세션만 지원합니다.");
        }

        long revision = Long.parseLong(parameters.getString("collectionRevision", "-1"));

        if (revision < 0) {
            throw new IllegalArgumentException("collectionRevision은 0 이상이어야 합니다.");
        }
        LocalDate date = LocalDate.parse(parameters.getString("tradeDate"));
        LocalTime start = LocalTime.parse(parameters.getString("startTime"));
        LocalTime end = LocalTime.parse(parameters.getString("endTime"));

        if (start.isAfter(end) || start.getSecond() != 0 || end.getSecond() != 0
                || start.getNano() != 0 || end.getNano() != 0) {
            throw new IllegalArgumentException("조회 구간은 분 단위의 오름차순 시각이어야 합니다.");
        }

        if (ZonedDateTime.now(clock).isBefore(date.atTime(end).atZone(SEOUL).plusMinutes(10))) {
            throw new IllegalArgumentException("수집 대상 구간 종료 후 10분이 지나야 합니다.");
        }

        return new MinutePriceWindow(date, start, end);
    }
}
