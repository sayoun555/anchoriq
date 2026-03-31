package com.anchoriq.core.domain.maritime.route.model;

import com.anchoriq.core.common.vo.Coordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Route 엔티티 테스트")
class RouteTest {

    private Chokepoint createChokepoint(String name, String riskLevel) {
        return Chokepoint.builder()
                .name(name)
                .displayName(name)
                .coordinate(Coordinate.of(0.0, 0.0))
                .riskLevel(riskLevel)
                .build();
    }

    private Route createRouteWithChokepoints(String routeName, int distanceNm,
                                              List<Chokepoint> chokepoints) {
        return Route.builder()
                .name(routeName)
                .displayName(routeName)
                .distanceNm(distanceNm)
                .chokepoints(chokepoints)
                .build();
    }

    @Nested
    @DisplayName("초크포인트 통과 여부 테스트")
    class PassesThroughTest {

        @Test
        @DisplayName("경유하는 초크포인트 이름이면 true를 반환한다")
        void should_returnTrue_when_routePassesThroughChokepoint() {
            // Given
            Route route = createRouteWithChokepoints("Asia-Europe", 8000,
                    List.of(createChokepoint("Suez", "HIGH"),
                            createChokepoint("Malacca", "MEDIUM")));

            // When & Then
            assertThat(route.passesThrough("Suez")).isTrue();
            assertThat(route.passesThrough("Malacca")).isTrue();
        }

        @Test
        @DisplayName("경유하지 않는 초크포인트이면 false를 반환한다")
        void should_returnFalse_when_routeDoesNotPassThrough() {
            Route route = createRouteWithChokepoints("Pacific", 6000,
                    List.of(createChokepoint("Panama", "LOW")));

            assertThat(route.passesThrough("Hormuz")).isFalse();
        }

        @Test
        @DisplayName("대소문자를 구분하지 않고 매칭한다")
        void should_matchCaseInsensitive_when_checkingChokepoint() {
            Route route = createRouteWithChokepoints("Asia-Europe", 8000,
                    List.of(createChokepoint("Suez", "HIGH")));

            assertThat(route.passesThrough("suez")).isTrue();
            assertThat(route.passesThrough("SUEZ")).isTrue();
        }
    }

    @Nested
    @DisplayName("고위험 항로 판단 테스트")
    class HighRiskTest {

        @Test
        @DisplayName("고위험 초크포인트가 포함되면 고위험 항로이다")
        void should_returnTrue_when_containsHighRiskChokepoint() {
            Route route = createRouteWithChokepoints("Asia-Europe", 8000,
                    List.of(createChokepoint("Hormuz", "HIGH"),
                            createChokepoint("Malacca", "MEDIUM")));

            assertThat(route.isHighRisk()).isTrue();
        }

        @Test
        @DisplayName("고위험 초크포인트가 없으면 고위험이 아니다")
        void should_returnFalse_when_noHighRiskChokepoint() {
            Route route = createRouteWithChokepoints("Safe Route", 3000,
                    List.of(createChokepoint("Gibraltar", "LOW")));

            assertThat(route.isHighRisk()).isFalse();
        }

        @Test
        @DisplayName("초크포인트가 없는 항로는 고위험이 아니다")
        void should_returnFalse_when_noChokepoints() {
            Route route = Route.create("Direct", "Direct Route", 2000);

            assertThat(route.isHighRisk()).isFalse();
        }
    }

    @Nested
    @DisplayName("리스크 스코어 계산 테스트")
    class RiskScoreCalculationTest {

        @Test
        @DisplayName("고위험 초크포인트당 +25, 중위험 이상당 +10으로 계산한다")
        void should_calculateScore_when_multipleChokepoints() {
            // Given
            Route route = createRouteWithChokepoints("High Risk", 6000,
                    List.of(createChokepoint("Hormuz", "HIGH"),
                            createChokepoint("Bab-el-Mandeb", "HIGH"),
                            createChokepoint("Malacca", "MEDIUM")));

            // When
            int score = route.calculateRiskScore(Set.of());

            // Then
            // 2 HIGH * 25 = 50 (chokepoint)
            // 3 MEDIUM_OR_HIGH * 10 = 30 (medium+)
            // 6000nm >= 5000 = +10 (distance)
            // total = 90
            assertThat(score).isGreaterThanOrEqualTo(50);
            assertThat(score).isLessThanOrEqualTo(100);
        }

        @Test
        @DisplayName("장거리 항로(5000nm 이상)는 +10을 받는다")
        void should_addTen_when_longDistanceRoute() {
            Route route = createRouteWithChokepoints("Long Route", 5000,
                    List.of());

            int score = route.calculateRiskScore(Set.of());

            assertThat(score).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("저위험 초크포인트만 있는 단거리 항로의 점수는 낮다")
        void should_returnLowScore_when_lowRiskShortRoute() {
            Route route = createRouteWithChokepoints("Safe", 2000,
                    List.of(createChokepoint("Gibraltar", "LOW")));

            int score = route.calculateRiskScore(Set.of());

            assertThat(score).isLessThan(25);
        }

        @Test
        @DisplayName("리스크 점수는 100을 초과하지 않는다")
        void should_capAtHundred_when_manyRiskFactors() {
            Route route = createRouteWithChokepoints("Very Risky", 8000,
                    List.of(createChokepoint("Hormuz", "HIGH"),
                            createChokepoint("Bab-el-Mandeb", "HIGH"),
                            createChokepoint("Suez", "HIGH"),
                            createChokepoint("Taiwan", "HIGH")));

            int score = route.calculateRiskScore(Set.of());

            assertThat(score).isEqualTo(100);
        }
    }

    @Test
    @DisplayName("차단된 초크포인트를 포함하면 대체 항로가 필요하다")
    void should_requireAlternative_when_blockedChokepointIncluded() {
        Route route = createRouteWithChokepoints("Suez Route", 8000,
                List.of(createChokepoint("Suez", "HIGH")));

        assertThat(route.requiresAlternative(Set.of("Suez"))).isTrue();
        assertThat(route.requiresAlternative(Set.of("Panama"))).isFalse();
    }

    @Test
    @DisplayName("초크포인트를 추가할 수 있다")
    void should_addChokepoint_when_notDuplicate() {
        Route route = Route.create("Test", "Test Route", 3000);

        route.addChokepoint(createChokepoint("Suez", "HIGH"));
        route.addChokepoint(createChokepoint("Malacca", "MEDIUM"));

        assertThat(route.getChokepoints().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("중복 초크포인트는 추가되지 않는다")
    void should_notAddDuplicate_when_chokepointAlreadyExists() {
        Route route = Route.create("Test", "Test Route", 3000);
        Chokepoint suez = createChokepoint("Suez", "HIGH");

        route.addChokepoint(suez);
        route.addChokepoint(suez);

        assertThat(route.getChokepoints().size()).isEqualTo(1);
    }
}
