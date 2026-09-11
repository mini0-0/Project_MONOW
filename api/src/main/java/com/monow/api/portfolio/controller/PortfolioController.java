package com.monow.api.portfolio.controller;

import com.monow.api.portfolio.application.PortfolioQueryService;
import com.monow.api.portfolio.dto.response.PortfolioResponse;
import com.monow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/users/me/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioQueryService portfolioQueryService;

    @GetMapping
    public ApiResponse<PortfolioResponse> getPortfolio() {
        // 임시 userId
        Long userId = 2L;

        PortfolioResponse response = portfolioQueryService.getPortfolio(userId);

        return ApiResponse.success(response);
    }


}
