package org.project.domain.memo.service;

import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoResponse;

import java.util.List;

public interface MemoService {

    MemoResponse createMemo(Long userId, MemoCreateRequest request);

    MemoListDashboardResponse getMemos(Long userId, List<Long> labelIds);
}
