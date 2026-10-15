package com.monow.api.security;

import com.monow.api.portfolio.application.PortfolioQueryService;
import com.monow.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.monow.global.security.jwt.JwtProperties;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAuthenticationTest {

    private static final String PROTECTED_URL = "/api/v1/users/me/portfolio";
    private static final Long USER_ID = 1L;
    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE  = "ADMIN";
    private static final String ADMIN_URL = "/api/v1/admin/test";


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private PortfolioQueryService portfolioQueryService;

    @Nested
    @DisplayName("401 Unauthorized - 인증 실패")
    class Unauthorized {

        @Test
        @DisplayName("[실패] - Access Token 없이 보호 API 요청 시 401 반환")
        void GivenNoAccessToken_WhenRequestProtectedApi_ThenUnauthorized() throws Exception {
            // Given

            // When & Then
            mockMvc.perform(get(PROTECTED_URL))
                    .andExpect(status().isUnauthorized());

        }

        @Test
        @DisplayName("[실패] - 유효하지 않은 Access Token으로 요청 시 401 반환")
        void GivenInvalidAccessToken_WhenRequestProtectedApi_ThenUnauthorized() throws Exception {
            // Given
            String invalidAccessToken = "invalid.access.token";

            // When & Then
            mockMvc.perform(get(PROTECTED_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidAccessToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("[실패] - 만료된 Access Token으로 요청 시 401 반환")
        void GivenExpiredAccessToken_WhenRequestProtectedApi_ThenUnauthorized() throws Exception {
            // Given
            String expiredAccessToken = createExpiredAccessToken();

            // When & Then
            mockMvc.perform(get(PROTECTED_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("인증 성공")
    class Authenticated {

        @Test
        @DisplayName("[성공] - 유효한 Access Token으로 보호 API 요청 시 인증 성공")
        void GivenValidAccessToken_WhenRequestProtectedApi_ThenAuthenticated() throws Exception {
            // Given
            String accessToken = jwtTokenProvider.createAccessToken(USER_ID, USER_ROLE);

            given(portfolioQueryService.getPortfolio(2L))
                    .willReturn(null);

            // When & Then
            mockMvc.perform(get(PROTECTED_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }


    @Nested
    @DisplayName("403 Forbidden - 인가 실패")
    class Forbidden {

        @Test
        @DisplayName("[실패] - USER 권한으로 관리자 API 접근 시 403 반환")
        void GivenUserRole_WhenRequestAdminApi_ThenForbidden() throws Exception {
            // Given
            String accessToken = jwtTokenProvider.createAccessToken(USER_ID, USER_ROLE);

            // When & Then
            mockMvc.perform(get(ADMIN_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("인가 성공")
    class Authorized {

        @Test
        @DisplayName("[성공] - ADMIN 권한으로 관리자 API 접근 시 200 반환")
        void GivenAdminRole_WhenRequestAdminApi_ThenSuccess() throws Exception {
            // Given
            String accessToken = jwtTokenProvider.createAccessToken(USER_ID, ADMIN_ROLE);

            // When & Then
            mockMvc.perform(get(ADMIN_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk());
        }
    }

    private String createExpiredAccessToken() {
        JwtProperties expiredJwtProperties = new JwtProperties(
                jwtProperties.secret(),
                Duration.ofSeconds(-60),
                jwtProperties.refreshTokenExpiration()
        );

        JwtTokenProvider expiredJwtTokenProvider = new JwtTokenProvider(expiredJwtProperties);

        return expiredJwtTokenProvider.createAccessToken(USER_ID, USER_ROLE);

    }

    @RestController
    static class TestAdminController {

        // 현재 실제 admin관련 controller 생성 안됨
        @GetMapping("/api/v1/admin/test")
        public ResponseEntity<Void> adminTest() {
            return ResponseEntity.ok().build();
        }
    }

}
