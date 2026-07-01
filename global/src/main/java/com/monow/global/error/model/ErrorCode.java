package com.monow.global.error.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * HTTP 상태 코드 사용 기준
 *
 * 400 Bad Request
 * - 클라이언트가 보낸 요청값 자체의 오류
 * - 요청값을 수정하면 해결 가능한 경우
 * - ex) 필수값 누락, 형식 오류, 유효하지 않은 파라미터, 잘못된 enum 값
 *
 * 401 Unauthorized
 * - 인증 정보 누락 또는 유효하지 않은 인증 정보
 * - 사용자를 확인할 수 없는 경우
 * - ex) 로그인 실패, Access Token 누락, 토큰 만료, 잘못된 Access Token
 *
 * 409 Conflict
 * - 요청 형식은 올바르지만 현재 서버 데이터 상태와의 충돌
 * - 중복 생성 또는 이미 처리된 요청
 * - ex) 이메일 중복, 이미 존재하는 계좌, 이미 등록된 종목, 이미 처리된 주문
 *
 * 502 Bad Gateway
 * - 외부 시스템 또는 외부 API 연동 실패
 * - 우리 서버가 의존하는 외부 시스템 문제
 * - ex) KIS API 호출 실패, 외부 API 실패 응답, 외부 응답 필드 누락, 외부 서버 장애
 *
 * 500 Internal Server Error
 * - 서버 내부에서 예상하지 못한 오류
 * - 개발자가 로그를 확인하고 수정해야 하는 서버 내부 문제
 * - ex) NullPointerException, 처리하지 못한 RuntimeException, 설정 누락, 서버 로직 오류
 */

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRM_MISMATCH", "비밀번호가 일치하지 않습니다."),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "DUPLICATED_EMAIL", "이미 가입된 이메일입니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "DUPLICATED_NICKNAME", "이미 사용 중인 닉네임입니다."),
    LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "LOGIN_FAIL", "이메일 또는 비밀번호가 일치하지 않습니다."),
    STOCK_NOT_FOUND(HttpStatus.BAD_REQUEST, "STOCK_NOT_FOUND", "존재하지 않은 종목코드입니다."),
    STOCK_INFO_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "STOCK_INFO_FETCH_FAILED", "한국투자 종목 정보 조회에 실패했습니다."),
    STOCK_MASTER_DOWNLOAD_FAILED(HttpStatus.BAD_GATEWAY,"STOCK_MASTER_DOWNLOAD_FAILED", "한국투자 국내주식 종목 파일 다운로드에 실패했습니다."),
    STOCK_MASTER_EXTRACT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STOCK_MASTER_EXTRACT_FAILED", "한국투자 국내주식 종목 파일 압축 해제에 실패했습니다."),
    STOCK_MASTER_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STOCK_MASTER_PARSE_FAILED", "한국투자 국내주식 종목 파일 파싱에 실패했습니다."),
    STOCK_DAILY_PRICE_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"STOCK_DAILY_PRICE_FETCH_FAILED" ,"한국투자 일봉 시세 조회에 실패했습니다." ),
    KIS_TOP_VIEW_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "KIS_TOP_VIEW_INVALID_RESPONSE", "HTS종목 상위 20 종목 응답 형식이 올바르지 않습니다."),
    KIS_TOP_VIEW_API_FAILED(HttpStatus.BAD_GATEWAY, "KIS_TOP_VIEW_API_FAILED", "HTS종목 상위 20 종목 응답 API 호출에 실패했습니다."),
    KIS_CURRENT_PRICE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "KIS_CURRENT_PRICE_FETCH_FAILED", "현재가 조회에 실패했습니다."),
    KIS_CURRENT_PRICE_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "KIS_CURRENT_PRICE_INVALID_RESPONSE", "현재가 응답 데이터가 올바르지 않습니다.");

    private final HttpStatus status;

    private final String code;

    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
