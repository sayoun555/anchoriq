package com.anchoriq.ai.query;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cypher 쿼리 보안 검증기.
 * 읽기 전용 쿼리만 허용하고, 데이터 변경 쿼리를 차단한다.
 */
public final class CypherQueryValidator {

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "CREATE", "DELETE", "DETACH", "SET", "REMOVE", "MERGE",
            "DROP", "CALL", "LOAD CSV", "FOREACH"
    );

    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "\\b(" + String.join("|", FORBIDDEN_KEYWORDS) + ")\\b",
            Pattern.CASE_INSENSITIVE
    );

    private CypherQueryValidator() {
    }

    /**
     * 쿼리가 읽기 전용(MATCH/RETURN)인지 검증한다.
     *
     * @param cypher 검증할 Cypher 쿼리
     * @return 읽기 전용이면 true
     */
    public static boolean isReadOnly(String cypher) {
        if (cypher == null || cypher.isBlank()) {
            return false;
        }
        return !FORBIDDEN_PATTERN.matcher(cypher).find();
    }

    /**
     * 쿼리가 유효한 읽기 전용 Cypher인지 검증한다.
     *
     * @param cypher 검증할 Cypher 쿼리
     * @throws IllegalArgumentException 쓰기 쿼리인 경우
     */
    public static void validateReadOnly(String cypher) {
        if (!isReadOnly(cypher)) {
            throw new IllegalArgumentException(
                    "Only read-only Cypher queries are allowed (MATCH/RETURN). " +
                    "Write operations (CREATE, DELETE, SET, MERGE) are forbidden.");
        }
    }
}
