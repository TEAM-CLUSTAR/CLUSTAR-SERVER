package org.project.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.UserException;
import org.project.global.exception.errorcode.UserErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoServiceImpl implements MemoService {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;

    @Transactional
    public MemoResponse createMemo(Long userId, MemoCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        Memo memo = Memo.createMemo(request.title(), request.content(), user);

        Memo savedMemo = memoRepository.save(memo);

        return MemoResponse.from(savedMemo);
    }

    @Override
    public MemoListDashboardResponse getMemos(Long userId, List<Long> labelIds) {

        List<Memo> memos;

        if (labelIds == null || labelIds.isEmpty()) {
            // 라벨 필터 없음 → 전체 조회
            memos = memoRepository.findAllByUserId(userId);
        } else {
            // 라벨 필터 있음 → 해당 라벨 포함 메모 조회
            memos = memoRepository.findAllByUserIdAndLabelIds(userId, labelIds);
        }

        return MemoListDashboardResponse.from(memos);
    }
}
