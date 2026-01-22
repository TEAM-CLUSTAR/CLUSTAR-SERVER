package org.project.domain.ai.rag.A.extract;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.A.extract.fileExtractor.MemoFileBinaryLoader;
import org.project.domain.ai.rag.A.extract.fileExtractor.dto.MemoFileBinary;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemoFileDocumentReader {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final MemoFileBinaryLoader memoFileBinaryLoader;
    private final MemoRepository memoRepository;

    public List<Document> read(
            Long memoId,
            List<Long> memoFileIds,
            Long userId
    ) {
        if (memoFileIds == null || memoFileIds.isEmpty()) {
            return List.of();
        }

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
        LocalDateTime memoCreatedAt = memo.getCreatedAt();

        List<Document> result = new ArrayList<>();

        for (Long fileId : memoFileIds) {

            MemoFileBinary file = memoFileBinaryLoader.load(fileId);


            // 추후 파일 크기를 제한하도록 수정
            if (file.fileSize() > MAX_FILE_SIZE) {
                continue;
            }

            // byte[] → Resource
            Resource resource = new ByteArrayResource(file.bytes()) {
                @Override
                public String getFilename() {
                    return file.fileName();
                }
            };

            // Tika Reader
            TikaDocumentReader reader = new TikaDocumentReader(resource);

            List<Document> documents;
            try {
                documents = reader.read();
            } catch (Exception e) {
                // 파일 하나 실패해도 파이프라인 유지
                continue;
            }

            // 4️⃣ Metadata Enrich
            for (Document doc : documents) {

                if (doc.getText() == null || doc.getText().isBlank()) {
                    continue;
                }

                doc.getMetadata().putAll(Map.of(
                        "type", RagDocumentType.MEMO_FILE.name(),
                        "memoId", memoId,
                        "userId", userId,
                        "fileId", fileId,
                        "fileName", file.fileName(),
                        "fileExtension", file.extension(),
                        "s3Key", file.s3Key(),
                        "createdAt",memoCreatedAt.toString()
                ));
                doc.getMetadata().remove("source");

                result.add(doc);
            }
        }

        return result;
    }
}
