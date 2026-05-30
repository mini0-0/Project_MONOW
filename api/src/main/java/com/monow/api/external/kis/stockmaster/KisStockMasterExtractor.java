package com.monow.api.external.kis.stockmaster;

import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.springframework.stereotype.Component;

import javax.imageio.IIOException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class KisStockMasterExtractor {

    public Map<DomesticStockMarketType, byte[]> extractMstFiles(
            Map<DomesticStockMarketType, byte[]> zipFiles
    ) {
        Map<DomesticStockMarketType, byte[]> result =
                new EnumMap<>(DomesticStockMarketType.class);

        for (Map.Entry<DomesticStockMarketType, byte[]> entry : zipFiles.entrySet()) {
            DomesticStockMarketType marketType = entry.getKey();
            byte[] zipBytes = entry.getValue();

            byte[] mstBytes = extractMstFile(zipBytes);

            result.put(marketType, mstBytes);
        }

        return result;
    }

    private byte[] extractMstFile(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw  new BusinessException(ErrorCode.STOCK_MASTER_EXTRACT_FAILED);
        }

        try (
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipBytes);
            ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream);
        ) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".mst")) {
                    return zipInputStream.readAllBytes();
                }
            }

            throw new BusinessException(ErrorCode.STOCK_MASTER_EXTRACT_FAILED);

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STOCK_MASTER_EXTRACT_FAILED);
        }

    }
}
