//package org.project.global.util.memoMedia;
//
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.text.PDFTextStripper;
//import org.springframework.stereotype.Component;
//
//@Component
//public class PdfTextExtractor {
//
//    public String extract(byte[] pdfBytes) {
//        try (PDDocument document = PDDocument.load(pdfBytes)) {
//            PDFTextStripper stripper = new PDFTextStripper();
//            return stripper.getText(document);
//        } catch (Exception e) {
//            throw new RuntimeException("PDF 텍스트 추출 실패", e);
//        }
//    }
//}
