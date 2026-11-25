package com.monow.external.dart.corpcode;

import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DartCorpCodeParser {

    private static final int MAX_ERROR_XML_BYTES = 16 * 1024;

    public List<CorpCodeEntry> parse(byte[] xml) throws IOException {
        List<CorpCodeEntry> entries = new ArrayList<>();

        try {
            var reader = createXmlFactory().createXMLStreamReader(new ByteArrayInputStream(xml));
            try {
                if (reader.nextTag() != XMLStreamConstants.START_ELEMENT || !"result".equals(reader.getLocalName())) {
                    throw new IOException("Open DART 고유번호 XML의 루트가 올바르지 않습니다.");
                }
                while (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
                    if (!"list".equals(reader.getLocalName())) {
                        throw new IOException("Open DART 고유번호 XML의 목록이 올바르지 않습니다.");
                    }

                    String stockCode = "";
                    String corpCode = "";

                    while (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
                        String name = reader.getLocalName();
                        String value = reader.getElementText().strip();
                        if ("stock_code".equals(name)) {
                            stockCode = value;
                        } else if ("corp_code".equals(name)) {
                            corpCode = value;
                        }
                    }
                    entries.add(new CorpCodeEntry(stockCode, corpCode));
                }
                while (reader.hasNext()) {
                    reader.next();
                }
            } finally {
                reader.close();
            }
        } catch (XMLStreamException exception) {
            throw new IOException("Open DART 고유번호 XML을 읽지 못했습니다.");
        }

        if (entries.isEmpty()) {
            throw new IOException("Open DART 고유번호 목록이 비어 있습니다.");
        }

        return List.copyOf(entries);
    }

    static String readErrorStatus(byte[] body) {
        if (body.length > MAX_ERROR_XML_BYTES) {
            return null;
        }

        try {
            var reader = createXmlFactory().createXMLStreamReader(new ByteArrayInputStream(body));
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT && "status".equals(reader.getLocalName())) {
                        String status = reader.getElementText().strip();
                        return status.matches("[0-9]{3}") ? status : null;
                    }
                }
            } finally {
                reader.close();
            }
        } catch (XMLStreamException exception) {
            return null;
        }
        return null;
    }

    private static XMLInputFactory createXmlFactory() {
        var factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }


    public record CorpCodeEntry(String stockCode, String corpCode) {
    }
}
