package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.common.vo.Coordinate;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.maritime.company.model.Company;
import com.anchoriq.core.domain.maritime.country.model.Country;
import com.anchoriq.core.domain.maritime.port.model.CongestionLevel;
import com.anchoriq.core.domain.maritime.port.model.Locode;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import com.anchoriq.core.domain.maritime.vessel.model.Flag;
import com.anchoriq.core.domain.maritime.vessel.model.Imo;
import com.anchoriq.core.domain.maritime.vessel.model.Mmsi;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.model.VesselStatus;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;
import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import com.anchoriq.core.domain.maritime.weather.model.WeatherType;
import com.anchoriq.core.domain.maritime.weather.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplyChainRiskService 도메인 서비스 테스트")
class SupplyChainRiskServiceTest {

    @Mock
    private SanctionRepository sanctionRepository;

    @Mock
    private WeatherRepository weatherRepository;

    private SupplyChainRiskService riskService;

    @BeforeEach
    void setUp() {
        riskService = new SupplyChainRiskServiceImpl(sanctionRepository, weatherRepository);
    }

    @Test
    @DisplayName("제재국 소유 선박의 리스크 점수가 높게 나온다")
    void should_returnHighRisk_when_vesselFromSanctionedCountry() {
        // Given
        Country iran = Country.createSanctioned("IR", "Iran", "Middle East");
        Company company = Company.create("Iranian Shipping", "REG-IR", iran);
        Vessel vessel = Vessel.builder()
                .imo(Imo.of("1234567"))
                .mmsi(Mmsi.of("123456789"))
                .name("IRAN VESSEL")
                .flag(Flag.of("IR"))
                .type(VesselType.TANKER)
                .status(VesselStatus.SAILING)
                .company(company)
                .buildYear(2000)
                .build();

        Sanction sanction = Sanction.create("REF-001", "IR", "COUNTRY",
                "UN", LocalDate.now().minusYears(5), null, "Iran sanctions");
        when(sanctionRepository.findActiveSanctions()).thenReturn(List.of(sanction));
        when(weatherRepository.findSevereConditions()).thenReturn(List.of());

        // When
        RiskScore score = riskService.assessVesselRisk(vessel);

        // Then
        assertThat(score.getScore()).isGreaterThan(0);
        assertThat(score.getTargetType()).isEqualTo("VESSEL");
        assertThat(score.getExplanation()).isNotBlank();
    }

    @Test
    @DisplayName("악천후 발생 시 리스크 점수에 반영된다")
    void should_increaseRisk_when_severeWeatherDetected() {
        // Given
        Vessel vessel = Vessel.builder()
                .imo(Imo.of("1234567"))
                .mmsi(Mmsi.of("123456789"))
                .name("SAFE VESSEL")
                .flag(Flag.of("KR"))
                .type(VesselType.CONTAINER)
                .status(VesselStatus.SAILING)
                .buildYear(2020)
                .build();

        when(sanctionRepository.findActiveSanctions()).thenReturn(List.of());
        WeatherCondition typhoon = WeatherCondition.create(
                WeatherType.TYPHOON, "HIGH", 30.0, 130.0, "Typhoon approaching");
        when(weatherRepository.findSevereConditions()).thenReturn(List.of(typhoon));

        // When
        RiskScore score = riskService.assessVesselRisk(vessel);

        // Then
        assertThat(score.getScore()).isGreaterThan(0);
        assertThat(score.getExplanation()).contains("weather");
    }

    @Test
    @DisplayName("고위험 초크포인트가 포함된 항로의 리스크가 높다")
    void should_returnHighRouteRisk_when_highRiskChokepoints() {
        // Given
        Chokepoint hormuz = Chokepoint.builder()
                .name("Hormuz")
                .displayName("Strait of Hormuz")
                .coordinate(Coordinate.of(26.59, 56.25))
                .riskLevel("HIGH")
                .build();
        Route route = Route.builder()
                .name("Persian Gulf Route")
                .displayName("Persian Gulf to Asia")
                .distanceNm(6000)
                .chokepoints(List.of(hormuz))
                .build();

        when(weatherRepository.findSevereConditions()).thenReturn(List.of());

        // When
        RiskScore score = riskService.assessRouteRisk(route);

        // Then
        assertThat(score.getScore()).isGreaterThan(0);
        assertThat(score.getTargetType()).isEqualTo("ROUTE");
    }

    @Test
    @DisplayName("위기적 혼잡도의 항만 리스크가 높다")
    void should_returnHighPortRisk_when_criticalCongestion() {
        // Given
        Port port = Port.builder()
                .locode(Locode.of("CNSHG"))
                .name("Shanghai")
                .country("CN")
                .coordinate(Coordinate.of(31.23, 121.47))
                .congestionLevel(CongestionLevel.of(95.0))
                .build();

        when(weatherRepository.findSevereConditions()).thenReturn(List.of());

        // When
        RiskScore score = riskService.assessPortRisk(port);

        // Then
        assertThat(score.getScore()).isGreaterThan(0);
        assertThat(score.getTargetType()).isEqualTo("PORT");
        assertThat(score.getExplanation()).contains("critical");
    }

    @Test
    @DisplayName("고위험 초크포인트의 리스크가 높다")
    void should_returnHighChokepointRisk_when_highGeopoliticalRisk() {
        // Given
        Chokepoint hormuz = Chokepoint.builder()
                .name("Hormuz")
                .displayName("Strait of Hormuz")
                .coordinate(Coordinate.of(26.59, 56.25))
                .riskLevel("HIGH")
                .transitVolume(150)
                .build();

        when(weatherRepository.findSevereConditions()).thenReturn(List.of());

        // When
        RiskScore score = riskService.assessChokepointRisk(hormuz);

        // Then
        assertThat(score.getScore()).isGreaterThan(0);
        assertThat(score.getTargetType()).isEqualTo("CHOKEPOINT");
    }
}
