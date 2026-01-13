package org.project.domain.memo.service;


import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;

public interface MemoS3Service {

    MemoPresignedUrlResponse issuePresignedUrls(
            Long userId,
            MemoPresignedUrlRequest request
    );
}
