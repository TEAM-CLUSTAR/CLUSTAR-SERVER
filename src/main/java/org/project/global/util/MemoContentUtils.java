package org.project.global.util;

public final class MemoContentUtils {

    private static final int DASHBOARD_CONTENT_MAX_LENGTH = 100;

    private MemoContentUtils() {
        // 유틸 클래스 생성 방지
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 대시보드용 메모 content를 최대 100자로 자른다.
     * - null 안전 처리
     * - 100자 초과 시 말줄임(...) 추가
     */
    public static String truncateForDashboard(String content) {
        if (content == null) {
            return null;
        }

        if (content.length() <= DASHBOARD_CONTENT_MAX_LENGTH) {
            return content;
        }

        return content.substring(0, DASHBOARD_CONTENT_MAX_LENGTH) + "...";
    }
}
