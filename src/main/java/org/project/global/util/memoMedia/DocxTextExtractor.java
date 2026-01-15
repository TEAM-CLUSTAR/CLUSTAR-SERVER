package org.project.global.util.memoMedia;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

@Component
public class DocxTextExtractor {

    public String extract(byte[] docxBytes) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            return doc.getParagraphs()
                    .stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("DOCX 텍스트 추출 실패", e);
        }
    }
}

