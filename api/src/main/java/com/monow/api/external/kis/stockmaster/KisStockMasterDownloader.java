package com.monow.api.external.kis.stockmaster;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.EnumMap;
import java.util.Map;

@Component
public class KisStockMasterDownloader {

    private final RestClient restClient;

    private final Map<DomesticStockMarketType, String> downloadUrls;

    public KisStockMasterDownloader() {
        this.restClient = RestClient.builder().build();
        this.downloadUrls = Map.of(
                DomesticStockMarketType.KOSPI, "https://new.real.download.dws.co.kr/common/master/kospi_code.mst.zip",
                DomesticStockMarketType.KOSDAQ,  "https://new.real.download.dws.co.kr/common/master/kosdaq_code.mst.zip",
                DomesticStockMarketType.NXT_KOSPI, "https://new.real.download.dws.co.kr/common/master/nxt_kospi_code.mst.zip",
                DomesticStockMarketType.NXT_KOSDAQ, "https://new.real.download.dws.co.kr/common/master/nxt_kosdaq_code.mst.zip"
        );


    }

    public Map<DomesticStockMarketType, byte[]> downloaderDomesticStock(){

        Map<DomesticStockMarketType, byte[]> result = new EnumMap<>(DomesticStockMarketType.class);

        for (Map.Entry<DomesticStockMarketType, String> entry : downloadUrls.entrySet()) {
            DomesticStockMarketType marketType = entry.getKey();
            String url = entry.getValue();

            byte[] zipBytes = download(url);

            result.put(marketType, zipBytes);

        }

        return result;

    }

    private byte[] download(String url) {
        byte[] zipBytes = restClient.get()
                .uri(url)
                .retrieve()
                .body(byte[].class);

        if (zipBytes == null || zipBytes.length == 0) {
            throw new BusinessException(ErrorCode.STOCK_MASTER_DOWNLOAD_FAILED);
        }

        return zipBytes;
    }

}
