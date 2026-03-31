package com.anchoriq.core.domain.maritime.sanction.model;

import com.anchoriq.core.domain.common.event.DomainEvent;
import com.anchoriq.core.domain.common.event.SanctionActivatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sanction 엔티티 테스트")
class SanctionTest {

    private Sanction createActiveSanction(String targetName, LocalDate startDate, LocalDate endDate) {
        return Sanction.create("REF-001", targetName, "COUNTRY", "UN",
                startDate, endDate, "Test sanction");
    }

    @Nested
    @DisplayName("활성 상태 판단 테스트")
    class IsCurrentlyActiveTest {

        @Test
        @DisplayName("시작일 이후이고 종료일 이전이면 활성 상태이다")
        void should_returnTrue_when_withinDateRange() {
            // Given
            Sanction sanction = createActiveSanction("Iran",
                    LocalDate.now().minusDays(30), LocalDate.now().plusDays(30));

            // Then
            assertThat(sanction.isCurrentlyActive()).isTrue();
        }

        @Test
        @DisplayName("종료일이 null이면 활성 상태이다 (무기한)")
        void should_returnTrue_when_noEndDate() {
            Sanction sanction = createActiveSanction("North Korea",
                    LocalDate.now().minusDays(365), null);

            assertThat(sanction.isCurrentlyActive()).isTrue();
        }

        @Test
        @DisplayName("시작일이 미래이면 아직 활성이 아니다")
        void should_returnFalse_when_startDateInFuture() {
            Sanction sanction = createActiveSanction("Future Target",
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            assertThat(sanction.isCurrentlyActive()).isFalse();
        }

        @Test
        @DisplayName("종료일이 과거이면 활성이 아니다")
        void should_returnFalse_when_endDateInPast() {
            Sanction sanction = createActiveSanction("Expired Target",
                    LocalDate.now().minusDays(100), LocalDate.now().minusDays(1));

            assertThat(sanction.isCurrentlyActive()).isFalse();
        }

        @Test
        @DisplayName("비활성화된 제재는 날짜와 관계없이 비활성이다")
        void should_returnFalse_when_deactivated() {
            Sanction sanction = createActiveSanction("Deactivated",
                    LocalDate.now().minusDays(30), LocalDate.now().plusDays(30));
            sanction.deactivate();

            assertThat(sanction.isCurrentlyActive()).isFalse();
        }

        @Test
        @DisplayName("시작일과 종료일 모두 null이면 활성 상태이다")
        void should_returnTrue_when_noDatesSet() {
            Sanction sanction = Sanction.create("REF-002", "Target", "ENTITY",
                    "UN", null, null, "No dates");

            assertThat(sanction.isCurrentlyActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("이름 매칭 테스트")
    class MatchesTest {

        @Test
        @DisplayName("정확히 일치하는 이름이면 true를 반환한다")
        void should_returnTrue_when_exactMatch() {
            Sanction sanction = createActiveSanction("Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matches("Iran")).isTrue();
        }

        @Test
        @DisplayName("대소문자를 무시하고 매칭한다")
        void should_returnTrue_when_caseInsensitiveMatch() {
            Sanction sanction = createActiveSanction("Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matches("iran")).isTrue();
            assertThat(sanction.matches("IRAN")).isTrue();
        }

        @Test
        @DisplayName("일치하지 않는 이름이면 false를 반환한다")
        void should_returnFalse_when_noMatch() {
            Sanction sanction = createActiveSanction("Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matches("North Korea")).isFalse();
        }

        @Test
        @DisplayName("null 또는 빈 문자열이면 false를 반환한다")
        void should_returnFalse_when_nullOrBlank() {
            Sanction sanction = createActiveSanction("Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matches(null)).isFalse();
            assertThat(sanction.matches("")).isFalse();
            assertThat(sanction.matches("   ")).isFalse();
        }

        @Test
        @DisplayName("공백을 트림하고 매칭한다")
        void should_trimAndMatch_when_inputHasWhitespace() {
            Sanction sanction = createActiveSanction("Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matches("  Iran  ")).isTrue();
        }
    }

    @Nested
    @DisplayName("부분 매칭 테스트")
    class MatchesPartialTest {

        @Test
        @DisplayName("부분 문자열로 매칭할 수 있다")
        void should_returnTrue_when_partialMatch() {
            Sanction sanction = createActiveSanction("Islamic Republic of Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matchesPartial("Iran")).isTrue();
            assertThat(sanction.matchesPartial("Islamic")).isTrue();
        }

        @Test
        @DisplayName("부분 매칭도 대소문자를 무시한다")
        void should_returnTrue_when_partialCaseInsensitive() {
            Sanction sanction = createActiveSanction("Islamic Republic of Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matchesPartial("iran")).isTrue();
            assertThat(sanction.matchesPartial("ISLAMIC")).isTrue();
        }

        @Test
        @DisplayName("포함되지 않는 문자열이면 false를 반환한다")
        void should_returnFalse_when_noPartialMatch() {
            Sanction sanction = createActiveSanction("Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matchesPartial("Korea")).isFalse();
        }

        @Test
        @DisplayName("null 또는 빈 문자열이면 false를 반환한다")
        void should_returnFalse_when_nullOrBlankQuery() {
            Sanction sanction = createActiveSanction("Iran",
                    LocalDate.now().minusDays(30), null);

            assertThat(sanction.matchesPartial(null)).isFalse();
            assertThat(sanction.matchesPartial("")).isFalse();
        }
    }

    @Test
    @DisplayName("활성화 시 SanctionActivatedEvent가 발행된다")
    void should_publishEvent_when_activated() {
        // Given
        Sanction sanction = createActiveSanction("Iran",
                LocalDate.now().minusDays(30), null);
        sanction.deactivate();
        sanction.clearDomainEvents();

        // When
        sanction.activate();

        // Then
        List<DomainEvent> events = sanction.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(SanctionActivatedEvent.class);

        SanctionActivatedEvent event = (SanctionActivatedEvent) events.get(0);
        assertThat(event.getTargetName()).isEqualTo("Iran");
    }
}
