package com.monow.external.dart.corpcode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DartCorpCodeParserTest {

    private final DartCorpCodeParser parser = new DartCorpCodeParser();

    @Test
    @DisplayName("[성공] - 종목코드의 선행 0을 유지하고 빈 종목코드를 반환한다")
    void givenCorpCodeXml_whenParse_thenKeepOriginalCodes() throws IOException {
        // Given
        byte[] xml = """
                <result>
                    <list><corp_code>00126380</corp_code><stock_code> 005930 </stock_code></list>
                    <list><corp_code>00000002</corp_code><stock_code> </stock_code></list>
                </result>
                """.getBytes(StandardCharsets.UTF_8);

        // When
        assertThat(parser.parse(xml))
                // Then
                .containsExactly(
                new DartCorpCodeParser.CorpCodeEntry("005930", "00126380"),
                new DartCorpCodeParser.CorpCodeEntry("", "00000002"));
    }

    @Test
    @DisplayName("[실패] - 손상된 XML의 부분 목록을 반환하지 않는다")
    void givenMalformedXml_whenParse_thenRejectEntireList() {
        // Given
        byte[] xml = "<result><list><corp_code>00126380</corp_code></list>".getBytes(StandardCharsets.UTF_8);

        // When
        assertThatThrownBy(() -> parser.parse(xml))
                // Then
                .isInstanceOf(IOException.class)
                .hasMessageContaining("XML");
    }
}
