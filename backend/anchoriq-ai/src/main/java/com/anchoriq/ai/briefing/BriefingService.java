package com.anchoriq.ai.briefing;

import java.util.Map;

/**
 * 자동 브리핑 서비스 인터페이스.
 * 매일 아침 리스크 서머리를 자동 생성한다.
 */
public interface BriefingService {

    Map<String, Object> generateDailyBriefing();

    Map<String, Object> generateWeeklyReport();
}
