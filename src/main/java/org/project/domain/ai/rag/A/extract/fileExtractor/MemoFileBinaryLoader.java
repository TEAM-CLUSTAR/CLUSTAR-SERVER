package org.project.domain.ai.rag.A.extract.fileExtractor;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.A.extract.fileExtractor.dto.MemoFileBinary;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.util.S3Util;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemoFileBinaryLoader {

    private final MemoFileRepository memoFileRepository;
    private final S3Util s3Util;

    public MemoFileBinary load(Long fileId) {

        MemoFile memoFile = memoFileRepository.findById(fileId)
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_FILE_NOT_FOUND));

        byte[] bytes = s3Util.download(memoFile.getFileS3Key());

        return new MemoFileBinary(
                bytes,
                memoFile.getFileName(),
                memoFile.getFileExtension(),
                memoFile.getFileBytes(),
                memoFile.getFileS3Key()
        );
    }
}
