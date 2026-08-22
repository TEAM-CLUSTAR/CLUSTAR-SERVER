package org.project.domain.memo.dto.response;

public enum SearchType {
    TEXT
    // [보존] 의미(벡터) 기반 검색은 기획 결정으로 비활성화됨.
    //        검색 화면에서 키워드/의미 결과를 UI로 구분하지 않고, 의미 검색이 직관적이지 않다는 판단.
    //        추후 의미 기반 검색을 다시 도입할 때 사용될 수 있어 값만 주석으로 남겨둔다.
    // , SEMANTIC
}
