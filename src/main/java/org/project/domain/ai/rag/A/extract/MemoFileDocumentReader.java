package org.project.domain.ai.rag.A.extract;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemoFileDocumentReader {

    public List<Document> read(
            Long memoId,
            List<Long> memoFileIds,
            Long userId
    ) {

        if (memoFileIds == null || memoFileIds.isEmpty()) {
            return List.of();
        }

        List<Document> documents = new ArrayList<>();

        for (Long fileId : memoFileIds) {

            // 지금 단계에서는 "파일 자체"가 아니라
            // "파일을 설명하는 텍스트 Document"를 만든다
            String fileTextRepresentation = """
                [FILE]
                fileId: %d
                memoId: %d
                """.formatted(fileId, memoId);

            Document document = new Document(
                    "memo-file-" + fileId,
                    fileTextRepresentation,
                    Map.of(
                            "type", "file",
                            "fileId", fileId,
                            "memoId", memoId,
                            "userId", userId
                    )
            );

            documents.add(document);
        }

        return documents;
    }
}
