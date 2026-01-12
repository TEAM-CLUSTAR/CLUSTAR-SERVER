package org.project.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.UserException;
import org.project.global.exception.errorcode.UserErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoServiceImpl implements MemoService {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;

    @Transactional
    public MemoResponse createMemo(Long userId, MemoCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        Memo memo = Memo.createMemo(request.title(), request.content(), user);

        if (request.labelNames() != null && !request.labelNames().isEmpty()) {
            for (int i = 0; i < request.labelNames().size(); i++) {
                String labelName = request.labelNames().get(i);

                // 해당 유저의 라벨 찾기 (없으면 생성)
                Label label = labelRepository.findByNameAndUser(labelName, user)
                        .orElseGet(() -> labelRepository.save(
                                Label.create(labelName, user)
                        ));

                // i가 우선순위임 (0, 1, 2, ...)
                memo.addLabel(label, i);
            }
        }

        Memo savedMemo = memoRepository.save(memo);

        return MemoResponse.from(savedMemo);
    }

    @Override
    public MemoListDashboardResponse getMemos(
            Long userId,
            List<Long> labelIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            int size
    ) {

        List<Memo> memos = memoRepository.findMemos(
                userId,
                labelIds,
                cursorCreatedAt,
                cursorMemoId,
                PageRequest.of(0, size)
        );

        return MemoListDashboardResponse.from(memos);
    }
}
