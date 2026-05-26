package com.monow.api.external.kis.mapper;

import com.monow.api.external.kis.dto.response.KisStockInfoResponse;
import com.monow.domain.stock.entity.Stock;
import org.springframework.stereotype.Component;

@Component
public class KisStockInfoMapper {

    public Stock toEntity(KisStockInfoResponse.Output output) {
        return Stock.createStock(
                output.productNumber(),
                output.standardProductNumber(),
                output.shortProductNumber(),
                output.productName(),
                output.productShortName(),
                "DOMESTIC_STOCK",
                output.productTypeCode(),
                output.productClassCode(),
                output.productClassName(),
                output.investmentProductTypeCode(),
                output.investmentProductTypeName()
        );


    }
}
