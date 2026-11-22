package org.project.global.exception.domainException;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.project.global.exception.errorcode.ReviewErrorCode;

// 글자 수 제한 위반. 왜 막혔는지(기준 몇 자)를 메시지와 응답 data에 담는다. 현재 글자 수는 알려주지 않는다.
// 기준값은 yml에서 바뀔 수 있어 메시지를 고정 문구가 아니라 실제 값으로 만든다
@Getter
public class ReviewLengthException extends ReviewException {

    private final String detailMessage;
    private final LengthDetail detail;

    private ReviewLengthException(ReviewErrorCode errorCode, String detailMessage, LengthDetail detail) {
        super(errorCode);
        this.detailMessage = detailMessage;
        this.detail = detail;
    }

    public static ReviewLengthException contentTooShort(int minLength) {
        return new ReviewLengthException(
                ReviewErrorCode.CONTENT_TOO_SHORT,
                "본문이 짧아 검토할 수 없어요. %d자 이상 작성해 주세요.".formatted(minLength),
                new LengthDetail(minLength, null)
        );
    }

    public static ReviewLengthException answerTooLong(int maxLength) {
        return new ReviewLengthException(
                ReviewErrorCode.ANSWER_TOO_LONG,
                "답변이 너무 길어요. %d자 이하로 줄여 주세요.".formatted(maxLength),
                new LengthDetail(null, maxLength)
        );
    }

    @Override
    public String getMessage() {
        return detailMessage;
    }

    // 응답 data. 해당하는 기준(min 또는 max)만 내려간다
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LengthDetail(Integer minLength, Integer maxLength) {
    }
}
