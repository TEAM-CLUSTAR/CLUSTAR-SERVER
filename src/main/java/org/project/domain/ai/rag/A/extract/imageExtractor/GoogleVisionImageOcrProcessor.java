package org.project.domain.ai.rag.A.extract.imageExtractor;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GoogleVisionImageOcrProcessor implements ImageOcrProcessor {

    @Override
    public String extractText(byte[] imageBytes) {

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {

            ByteString imgBytes = ByteString.copyFrom(imageBytes);

            Image image = Image.newBuilder()
                    .setContent(imgBytes)
                    .build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            List<AnnotateImageResponse> responses =
                    client.batchAnnotateImages(List.of(request)).getResponsesList();

            if (responses.isEmpty()) {
                return null;
            }

            AnnotateImageResponse response = responses.get(0);

            if (response.hasError()) {
                log.error("Google Vision OCR error: {}", response.getError().getMessage());
                return null;
            }

            // 전체 OCR 텍스트 (가장 중요)
            TextAnnotation annotation = response.getFullTextAnnotation();

            if (annotation == null || annotation.getText().isBlank()) {
                return null;
            }

            return annotation.getText().trim();

        } catch (Exception e) {
            log.error("OCR processing failed", e);
            return null;
        }
    }
}
