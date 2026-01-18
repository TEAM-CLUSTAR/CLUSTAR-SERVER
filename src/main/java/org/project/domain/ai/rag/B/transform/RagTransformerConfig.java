package org.project.domain.ai.rag.B.transform;

import org.project.domain.ai.rag.B.transform.file.FileMetadataEnricherTransformer;
import org.project.domain.ai.rag.B.transform.file.FileTextCleanupTransformer;
import org.project.domain.ai.rag.B.transform.file.FileTokenTextSplitterTransformer;
import org.project.domain.ai.rag.B.transform.image.MemoImageChunkTransformer;
import org.project.domain.ai.rag.B.transform.image.MemoImageOcrNormalizeTransformer;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagTransformerConfig {

    @Bean
    @Qualifier("fileTransformers")
    public List<DocumentTransformer> fileTransformers(
            FileTextCleanupTransformer cleanup,
            FileTokenTextSplitterTransformer splitter,
            FileMetadataEnricherTransformer metadata
    ) {
        return List.of(cleanup, splitter, metadata);
    }

    @Bean
    @Qualifier("imageTransformers")
    public List<DocumentTransformer> imageTransformers(
            MemoImageOcrNormalizeTransformer ocr,
            MemoImageChunkTransformer chunk
    ) {
        return List.of(ocr, chunk);
    }
}

