package com.anchoriq.core.domain.maritime.vessel.model;

import com.anchoriq.core.domain.common.event.DomainEvent;
import com.anchoriq.core.domain.common.event.RiskScoreChangedEvent;
import com.anchoriq.core.domain.common.event.VesselStatusChangedEvent;
import com.anchoriq.core.domain.maritime.company.model.Company;
import com.anchoriq.core.domain.maritime.country.model.Country;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Vessel 엔티티 테스트")
class VesselTest {

    private Vessel createDefaultVessel() {
        return Vessel.builder()
                .imo(Imo.of("1234567"))
                .mmsi(Mmsi.of("123456789"))
                .name("TEST VESSEL")
                .flag(Flag.of("KR"))
                .type(VesselType.CONTAINER)
                .status(VesselStatus.SAILING)
                .buildYear(2010)
                .deadweight(50000)
                .build();
    }

    private Vessel createVesselWithCompanyInCountry(String countryCode) {
        Country country = Country.create(countryCode, "Test Country");
        Company company = Company.create("Test Company", "REG-001", country);
        return Vessel.builder()
                .imo(Imo.of("1234567"))
                .mmsi(Mmsi.of("123456789"))
                .name("TEST VESSEL")
                .flag(Flag.of("KR"))
                .type(VesselType.TANKER)
                .status(VesselStatus.SAILING)
                .company(company)
                .buildYear(2000)
                .build();
    }

    @Nested
    @DisplayName("Builder 생성 테스트")
    class BuilderTest {

        @Test
        @DisplayName("Builder로 선박을 생성할 수 있다")
        void should_createVessel_when_allRequiredFieldsProvided() {
            Vessel vessel = createDefaultVessel();

            assertThat(vessel.getImo().value()).isEqualTo("1234567");
            assertThat(vessel.getMmsi().value()).isEqualTo("123456789");
            assertThat(vessel.getName()).isEqualTo("TEST VESSEL");
            assertThat(vessel.getFlag().value()).isEqualTo("KR");
            assertThat(vessel.getType()).isEqualTo(VesselType.CONTAINER);
            assertThat(vessel.getStatus()).isEqualTo(VesselStatus.SAILING);
        }

        @Test
        @DisplayName("status 미지정 시 UNKNOWN이 기본값이다")
        void should_defaultToUnknown_when_statusNotProvided() {
            Vessel vessel = Vessel.builder()
                    .imo(Imo.of("1234567"))
                    .mmsi(Mmsi.of("123456789"))
                    .name("TEST")
                    .flag(Flag.of("KR"))
                    .type(VesselType.CONTAINER)
                    .build();

            assertThat(vessel.getStatus()).isEqualTo(VesselStatus.UNKNOWN);
        }

        @Test
        @DisplayName("IMO가 null이면 NullPointerException을 던진다")
        void should_throwNpe_when_imoNull() {
            assertThatThrownBy(() -> Vessel.builder()
                    .mmsi(Mmsi.of("123456789"))
                    .name("TEST")
                    .flag(Flag.of("KR"))
                    .type(VesselType.CONTAINER)
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("IMO");
        }

        @Test
        @DisplayName("초기 리스크 점수는 0이다")
        void should_initializeRiskScoreToZero_when_created() {
            Vessel vessel = createDefaultVessel();
            assertThat(vessel.getRiskScore()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("상태 머신 전이 테스트")
    class StatusTransitionTest {

        @Test
        @DisplayName("SAILING에서 MOORED로 전이할 수 있다")
        void should_changeStatus_when_sailingToMoored() {
            Vessel vessel = createDefaultVessel();
            vessel.changeStatus(VesselStatus.MOORED);

            assertThat(vessel.getStatus()).isEqualTo(VesselStatus.MOORED);
        }

        @Test
        @DisplayName("SAILING에서 ANCHORED로 전이할 수 있다")
        void should_changeStatus_when_sailingToAnchored() {
            Vessel vessel = createDefaultVessel();
            vessel.changeStatus(VesselStatus.ANCHORED);

            assertThat(vessel.getStatus()).isEqualTo(VesselStatus.ANCHORED);
        }

        @Test
        @DisplayName("MOORED에서 SAILING으로 전이할 수 있다")
        void should_changeStatus_when_mooredToSailing() {
            Vessel vessel = createDefaultVessel();
            vessel.changeStatus(VesselStatus.MOORED);
            vessel.clearDomainEvents();
            vessel.changeStatus(VesselStatus.SAILING);

            assertThat(vessel.getStatus()).isEqualTo(VesselStatus.SAILING);
        }

        @Test
        @DisplayName("DECOMMISSIONED에서는 다른 상태로 전이할 수 없다")
        void should_throwException_when_decommissionedToAnyStatus() {
            Vessel vessel = createDefaultVessel();
            vessel.changeStatus(VesselStatus.DECOMMISSIONED);

            assertThatThrownBy(() -> vessel.changeStatus(VesselStatus.SAILING))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid vessel status transition");
        }

        @Test
        @DisplayName("ANCHORED에서 MOORED로 직접 전이할 수 없다")
        void should_throwException_when_anchoredToMoored() {
            Vessel vessel = createDefaultVessel();
            vessel.changeStatus(VesselStatus.ANCHORED);

            assertThatThrownBy(() -> vessel.changeStatus(VesselStatus.MOORED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid vessel status transition");
        }

        @Test
        @DisplayName("상태 변경 시 VesselStatusChangedEvent가 발행된다")
        void should_publishEvent_when_statusChanged() {
            Vessel vessel = createDefaultVessel();
            vessel.clearDomainEvents();

            vessel.changeStatus(VesselStatus.MOORED);

            List<DomainEvent> events = vessel.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(VesselStatusChangedEvent.class);

            VesselStatusChangedEvent event = (VesselStatusChangedEvent) events.get(0);
            assertThat(event.getVesselImo()).isEqualTo("1234567");
            assertThat(event.getPreviousStatus()).isEqualTo("SAILING");
            assertThat(event.getNewStatus()).isEqualTo("MOORED");
        }
    }

    @Nested
    @DisplayName("리스크 평가 테스트")
    class RiskEvaluationTest {

        @Test
        @DisplayName("제재국 회사 소유 선박은 높은 리스크 점수를 받는다")
        void should_returnHighScore_when_sanctionedCountryVessel() {
            // Given
            Vessel vessel = createVesselWithCompanyInCountry("IR");
            Set<String> sanctionedCodes = Set.of("IR", "KP", "SY");
            Set<String> highRiskFlags = Set.of();

            // When
            int score = vessel.evaluateRiskScore(sanctionedCodes, highRiskFlags);

            // Then (+40 제재국 + 선령점수 + 탱커점수 가능)
            assertThat(score).isGreaterThanOrEqualTo(40);
        }

        @Test
        @DisplayName("고위험 국기 선박은 +20 점수를 받는다")
        void should_addTwenty_when_highRiskFlag() {
            // Given
            Vessel vessel = Vessel.builder()
                    .imo(Imo.of("1234567"))
                    .mmsi(Mmsi.of("123456789"))
                    .name("TEST")
                    .flag(Flag.of("PA"))
                    .type(VesselType.CONTAINER)
                    .status(VesselStatus.SAILING)
                    .buildYear(2020)
                    .build();
            Set<String> sanctionedCodes = Set.of();
            Set<String> highRiskFlags = Set.of("PA", "LR", "MH");

            // When
            int score = vessel.evaluateRiskScore(sanctionedCodes, highRiskFlags);

            // Then
            assertThat(score).isGreaterThanOrEqualTo(20);
        }

        @Test
        @DisplayName("탱커는 +10 추가 리스크를 받는다")
        void should_addTen_when_tankerType() {
            // Given
            Vessel vessel = Vessel.builder()
                    .imo(Imo.of("1234567"))
                    .mmsi(Mmsi.of("123456789"))
                    .name("TEST TANKER")
                    .flag(Flag.of("KR"))
                    .type(VesselType.TANKER)
                    .status(VesselStatus.SAILING)
                    .buildYear(2020)
                    .build();

            // When
            int score = vessel.evaluateRiskScore(Set.of(), Set.of());

            // Then
            assertThat(score).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("UNKNOWN 상태 선박은 +15 리스크를 받는다")
        void should_addFifteen_when_unknownStatus() {
            // Given
            Vessel vessel = Vessel.builder()
                    .imo(Imo.of("1234567"))
                    .mmsi(Mmsi.of("123456789"))
                    .name("TEST")
                    .flag(Flag.of("KR"))
                    .type(VesselType.CONTAINER)
                    .buildYear(2020)
                    .build(); // default UNKNOWN

            // When
            int score = vessel.evaluateRiskScore(Set.of(), Set.of());

            // Then
            assertThat(score).isGreaterThanOrEqualTo(15);
        }

        @Test
        @DisplayName("리스크 점수 변경 시 RiskScoreChangedEvent가 발행된다")
        void should_publishEvent_when_riskScoreChanged() {
            // Given
            Vessel vessel = Vessel.builder()
                    .imo(Imo.of("1234567"))
                    .mmsi(Mmsi.of("123456789"))
                    .name("TEST")
                    .flag(Flag.of("PA"))
                    .type(VesselType.TANKER)
                    .status(VesselStatus.UNKNOWN)
                    .buildYear(2000)
                    .build();
            vessel.clearDomainEvents();

            // When
            vessel.evaluateRiskScore(Set.of(), Set.of("PA"));

            // Then
            List<DomainEvent> events = vessel.getDomainEvents();
            assertThat(events).isNotEmpty();
            assertThat(events.stream().anyMatch(e -> e instanceof RiskScoreChangedEvent)).isTrue();
        }

        @Test
        @DisplayName("리스크 점수는 100을 초과하지 않는다")
        void should_capAtHundred_when_manyRiskFactors() {
            // Given - 모든 리스크 요소를 가진 선박 (제재국 + 고위험 국기 + 고령)
            Vessel vessel = createVesselWithCompanyInCountry("IR");
            vessel.clearDomainEvents();

            Set<String> sanctionedCodes = Set.of("IR");
            Set<String> highRiskFlags = Set.of("KR");

            // When
            int score = vessel.evaluateRiskScore(sanctionedCodes, highRiskFlags);

            // Then
            assertThat(score).isLessThanOrEqualTo(100);
        }
    }

    @Nested
    @DisplayName("제재국 판단 테스트")
    class SanctionCheckTest {

        @Test
        @DisplayName("제재국에 등록된 회사 소유 선박이면 true를 반환한다")
        void should_returnTrue_when_registeredInSanctionedCountry() {
            Vessel vessel = createVesselWithCompanyInCountry("IR");

            boolean result = vessel.isRegisteredInSanctionedCountry(Set.of("IR", "KP"));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("비제재국에 등록된 회사 소유 선박이면 false를 반환한다")
        void should_returnFalse_when_notRegisteredInSanctionedCountry() {
            Vessel vessel = createVesselWithCompanyInCountry("KR");

            boolean result = vessel.isRegisteredInSanctionedCountry(Set.of("IR", "KP"));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("회사 정보가 없는 선박이면 false를 반환한다")
        void should_returnFalse_when_noCompanyInfo() {
            Vessel vessel = createDefaultVessel();

            boolean result = vessel.isRegisteredInSanctionedCountry(Set.of("IR", "KP"));

            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("동일한 IMO를 가진 선박은 동등하다")
    void should_beEqual_when_sameImo() {
        Vessel vessel1 = createDefaultVessel();
        Vessel vessel2 = Vessel.builder()
                .imo(Imo.of("1234567"))
                .mmsi(Mmsi.of("999999999"))
                .name("OTHER VESSEL")
                .flag(Flag.of("US"))
                .type(VesselType.BULK_CARRIER)
                .build();

        assertThat(vessel1).isEqualTo(vessel2);
    }
}
