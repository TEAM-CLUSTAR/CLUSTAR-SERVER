package org.project.domain.ai.rag.B.transform;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemoImageDocumentTransformer implements DocumentTransformer {

    @Override
    public List<Document> apply(List<Document> documents) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {

            // ✅ Text Document만 처리
            if (!doc.isText()) {
                continue;
            }

            String text = doc.getText();

            if (text == null || text.isBlank()) {
                continue; // OCR 실패 or 무의미 텍스트
            }

            String enriched = """
            [IMAGE OCR CONTENT]
            %s
            """.formatted(text);

            Document transformed = doc.mutate()
                    .text(enriched)
                    .build();

            result.add(transformed);
        }

        return result;
    }


//    @Override
//    public List<Document> apply(List<Document> documents) {
//        if (documents == null || documents.isEmpty()) {
//            return List.of();
//        }
//
//        List<Document> transformed = new ArrayList<>();
//
//        for (Document doc : documents) {
//            Map<String, Object> metadata = doc.getMetadata();
//
//            // 이미지 식별 정보
//            String imageName = (String) metadata.getOrDefault("fileName", "unknown-image");
//            String imageType = (String) metadata.getOrDefault("contentType", "image");
//
//            // VectorStore에 들어갈 텍스트 표현
//            String imageTextRepresentation = """
//                [IMAGE]
//                fileName: %s
//                type: %s
//                """.formatted(imageName, imageType);
//
//            Document imageDocument = new Document(
//                    doc.getId(),
//                    imageTextRepresentation,
//                    metadata
//            );
//
//            transformed.add(imageDocument);
//        }
//
//        return transformed;
//    }

//    public List<Document> transform(List<Document> documents) {
//        return apply(documents);
//    }
}
