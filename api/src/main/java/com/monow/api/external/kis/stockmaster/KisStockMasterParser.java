package com.monow.api.external.kis.stockmaster;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class KisStockMasterParser {
    public Map<DomesticStockMarketType, List<String>> parseStockCodes(
            Map<DomesticStockMarketType,byte[]> mstFiles
    ) {
        Map<DomesticStockMarketType, List<String>> result =
                new EnumMap<>(DomesticStockMarketType.class);

        for (Map.Entry<DomesticStockMarketType, byte[]> entry : mstFiles.entrySet()) {
            DomesticStockMarketType marketType = entry.getKey();
            byte[] mstBytes = entry.getValue();

            List<String> stockCodes = parseStockCodes(mstBytes);

            result.put(marketType, stockCodes);
        }

        return result;

    }

    private List<String> parseStockCodes(byte[] mstBytes) {
        if (mstBytes == null || mstBytes.length == 0) {
            throw new BusinessException(ErrorCode.STOCK_MASTER_PARSE_FAILED);
        }

        List<String> stockCodes = new ArrayList<>();

        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mstBytes);
                InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        ) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                if (line.length() < 6) {
                    continue;
                }

                String stockCode = line.substring(0, 6).trim();

                if (stockCode.matches("\\d{6}")) {
                    stockCodes.add(stockCode);
                }

            }
            return stockCodes;

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STOCK_MASTER_PARSE_FAILED);

        }

    }

    private boolean isValidStockCode (String stockCode){
        return stockCode != null && stockCode.matches("\\d{6}");
    }
}
