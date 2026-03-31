package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.common.vo.Coordinate;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RouteOptimizationService 도메인 서비스 테스트")
class RouteOptimizationServiceTest {

    @Mock
    private RouteRepository routeRepository;

    private RouteOptimizationService optimizationService;

    @BeforeEach
    void setUp() {
        optimizationService = new RouteOptimizationServiceImpl(routeRepository);
    }

    private Chokepoint createChokepoint(String name, String riskLevel) {
        return Chokepoint.builder()
                .name(name)
                .displayName(name)
                .coordinate(Coordinate.of(0.0, 0.0))
                .riskLevel(riskLevel)
                .build();
    }

    @Test
    @DisplayName("특정 초크포인트를 경유하지 않는 대체 항로를 찾는다")
    void should_findAlternativeRoutes_when_chokepointBlocked() {
        // Given
        Route suezRoute = Route.builder()
                .name("Suez Route")
                .distanceNm(8000)
                .chokepoints(List.of(createChokepoint("Suez", "HIGH")))
                .build();
        Route capeRoute = Route.builder()
                .name("Cape Route")
                .distanceNm(12000)
                .chokepoints(List.of(createChokepoint("Cape of Good Hope", "LOW")))
                .build();
        Route panamaRoute = Route.builder()
                .name("Panama Route")
                .distanceNm(10000)
                .chokepoints(List.of(createChokepoint("Panama", "MEDIUM")))
                .build();

        when(routeRepository.findAll()).thenReturn(List.of(suezRoute, capeRoute, panamaRoute));

        // When
        List<Route> alternatives = optimizationService.findAlternativeRoutes("Suez");

        // Then
        assertThat(alternatives).hasSize(2);
        assertThat(alternatives).extracting(Route::getName)
                .containsExactlyInAnyOrder("Cape Route", "Panama Route");
    }

    @Test
    @DisplayName("모든 항로가 해당 초크포인트를 경유하면 빈 목록을 반환한다")
    void should_returnEmptyList_when_allRoutesPassThroughChokepoint() {
        // Given
        Route route1 = Route.builder()
                .name("Route 1")
                .distanceNm(8000)
                .chokepoints(List.of(createChokepoint("Suez", "HIGH")))
                .build();

        when(routeRepository.findAll()).thenReturn(List.of(route1));

        // When
        List<Route> alternatives = optimizationService.findAlternativeRoutes("Suez");

        // Then
        assertThat(alternatives).isEmpty();
    }

    @Test
    @DisplayName("항로 비용 분석 결과를 반환한다")
    void should_returnCostAnalysis_when_routeProvided() {
        // Given
        Route route = Route.builder()
                .name("Asia-Europe via Suez")
                .distanceNm(8400)
                .chokepoints(List.of(createChokepoint("Suez", "HIGH")))
                .build();

        // When
        Map<String, Object> analysis = optimizationService.analyzeRouteCost(route);

        // Then
        assertThat(analysis).containsKey("routeName");
        assertThat(analysis).containsKey("distanceNm");
        assertThat(analysis).containsKey("estimatedDays");
        assertThat(analysis).containsKey("highRiskChokepoints");
        assertThat(analysis.get("routeName")).isEqualTo("Asia-Europe via Suez");
        assertThat(analysis.get("distanceNm")).isEqualTo(8400);
        assertThat(analysis.get("isHighRisk")).isEqualTo(true);
    }

    @Test
    @DisplayName("가장 안전한 항로를 추천한다 (고위험 초크포인트 최소)")
    void should_recommendSafestRoute_when_multipleCandidates() {
        // Given
        Route dangerousRoute = Route.builder()
                .name("Dangerous")
                .distanceNm(8000)
                .chokepoints(List.of(
                        createChokepoint("Hormuz", "HIGH"),
                        createChokepoint("Bab-el-Mandeb", "HIGH")))
                .build();
        Route safeRoute = Route.builder()
                .name("Safe")
                .distanceNm(12000)
                .chokepoints(List.of(createChokepoint("Cape", "LOW")))
                .build();
        Route mediumRoute = Route.builder()
                .name("Medium")
                .distanceNm(10000)
                .chokepoints(List.of(createChokepoint("Suez", "HIGH")))
                .build();

        // When
        Route recommended = optimizationService.recommendSafestRoute(
                List.of(dangerousRoute, safeRoute, mediumRoute));

        // Then
        assertThat(recommended.getName()).isEqualTo("Safe");
    }

    @Test
    @DisplayName("후보 항로가 비어있으면 null을 반환한다")
    void should_returnNull_when_noCandidates() {
        Route recommended = optimizationService.recommendSafestRoute(List.of());

        assertThat(recommended).isNull();
    }
}
