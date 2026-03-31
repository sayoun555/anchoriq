package com.anchoriq.ai.briefing;

import com.anchoriq.ai.client.AiClient;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyType;
import com.anchoriq.core.domain.intelligence.anomaly.repository.AnomalyRepository;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import com.anchoriq.core.domain.maritime.weather.repository.WeatherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 일일 브리핑 서비스 구현체.
 * OpenClaw를 활용하여 자연어 브리핑을 생성한다.
 */
@Slf4j
@Service
public class DailyBriefingServiceImpl implements BriefingService {

    private static final String BRIEFING_PROMPT = """
            You are a maritime intelligence briefing officer. Generate a concise daily risk briefing
            based on the provided data. Include:
            1. Executive summary (2-3 sentences)
            2. Key highlights (top 3-5 risk events)
            3. Recommended actions

            Keep it professional, actionable, and under 500 words.
            """;

    private final AiClient aiClient;
    private final VesselRepository vesselRepository;
    private final ChokepointRepository chokepointRepository;
    private final WeatherRepository weatherRepository;
    private final AnomalyRepository anomalyRepository;

    public DailyBriefingServiceImpl(AiClient aiClient,
                                     VesselRepository vesselRepository,
                                     ChokepointRepository chokepointRepository,
                                     WeatherRepository weatherRepository,
                                     AnomalyRepository anomalyRepository) {
        this.aiClient = aiClient;
        this.vesselRepository = vesselRepository;
        this.chokepointRepository = chokepointRepository;
        this.weatherRepository = weatherRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @Override
    public Map<String, Object> generateDailyBriefing() {
        Map<String, Object> briefing = new HashMap<>();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        briefing.put("date", today);

        Map<String, Object> statistics = collectStatistics();
        briefing.put("statistics", statistics);

        List<Map<String, Object>> highlights = collectHighlights();
        briefing.put("highlights", highlights);

        String summary = generateAiBriefingSummary(statistics, highlights);
        briefing.put("summary", summary);

        return briefing;
    }

    @Override
    public Map<String, Object> generateWeeklyReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("period", "Weekly");
        report.put("generatedAt", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        Map<String, Object> statistics = collectStatistics();
        report.put("statistics", statistics);

        List<Map<String, Object>> highlights = collectHighlights();
        report.put("highlights", highlights);

        String summary = aiClient.chat(
                BRIEFING_PROMPT.replace("daily", "weekly"),
                formatDataForAi(statistics, highlights)
        ).block();
        report.put("summary", summary);

        return report;
    }

    private Map<String, Object> collectStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVessels", vesselRepository.count());

        long aisOffCount = anomalyRepository.countByType(AnomalyType.AIS_OFF);
        stats.put("aisOffVessels", aisOffCount);

        long darkShipCount = anomalyRepository.countByType(AnomalyType.DARK_SHIP);
        stats.put("darkShips", darkShipCount);

        long routeDeviationCount = anomalyRepository.countByType(AnomalyType.ROUTE_DEVIATION);
        stats.put("routeDeviations", routeDeviationCount);

        long speedAnomalyCount = anomalyRepository.countByType(AnomalyType.SPEED_CHANGE);
        stats.put("speedAnomalies", speedAnomalyCount);

        List<Chokepoint> highRiskChokepoints = chokepointRepository.findHighRisk();
        stats.put("highRiskChokepoints", highRiskChokepoints.size());

        List<WeatherCondition> severeWeather = weatherRepository.findSevereConditions();
        stats.put("severeWeatherEvents", severeWeather.size());

        return stats;
    }

    private List<Map<String, Object>> collectHighlights() {
        List<Map<String, Object>> highlights = new ArrayList<>();

        chokepointRepository.findHighRisk().forEach(cp -> {
            Map<String, Object> highlight = new HashMap<>();
            highlight.put("type", "CHOKEPOINT_RISK");
            highlight.put("title", "High risk at " + cp.getDisplayName());
            highlight.put("description", cp.getDescription());
            highlight.put("riskLevel", cp.getRiskLevel());
            highlights.add(highlight);
        });

        weatherRepository.findSevereConditions().stream().limit(3).forEach(wc -> {
            Map<String, Object> highlight = new HashMap<>();
            highlight.put("type", "WEATHER_ALERT");
            highlight.put("title", wc.getType() + " - " + wc.getSeverity());
            highlight.put("description", wc.getDescription());
            highlight.put("riskLevel", wc.getSeverity());
            highlights.add(highlight);
        });

        return highlights;
    }

    private String generateAiBriefingSummary(Map<String, Object> statistics,
                                              List<Map<String, Object>> highlights) {
        String dataMessage = formatDataForAi(statistics, highlights);
        String summary = aiClient.chat(BRIEFING_PROMPT, dataMessage).block();
        return summary != null ? summary : "Briefing generation unavailable.";
    }

    private String formatDataForAi(Map<String, Object> statistics,
                                    List<Map<String, Object>> highlights) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Statistics:\n");
        statistics.forEach((key, value) -> sb.append("- ").append(key).append(": ").append(value).append("\n"));
        sb.append("\nKey Events:\n");
        highlights.forEach(h -> sb.append("- ").append(h.get("title")).append(": ").append(h.get("description")).append("\n"));
        return sb.toString();
    }
}
