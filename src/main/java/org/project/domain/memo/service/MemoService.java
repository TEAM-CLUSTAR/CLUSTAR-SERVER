package org.project.domain.memo.service;

import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoResponse;

public interface MemoService {

    MemoResponse createMemo(Long userId, MemoCreateRequest request);
}
