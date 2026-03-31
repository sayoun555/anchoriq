package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.maritime.company.model.Company;
import com.anchoriq.core.domain.maritime.country.model.Country;
import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import com.anchoriq.core.domain.maritime.vessel.model.Flag;
import com.anchoriq.core.domain.maritime.vessel.model.Imo;
import com.anchoriq.core.domain.maritime.vessel.model.Mmsi;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SanctionScreeningService 도메인 서비스 테스트")
class SanctionScreeningServiceTest {

    @Mock
    private SanctionRepository sanctionRepository;

    @Mock
    private VesselRepository vesselRepository;

    private SanctionScreeningService screeningService;

    @BeforeEach
    void setUp() {
        screeningService = new SanctionScreeningServiceImpl(sanctionRepository, vesselRepository);
    }

    @Test
    @DisplayName("제재국 소유 선박이면 제재 대상으로 판단한다")
    void should_returnTrue_when_vesselFromSanctionedCountry() {
        // Given
        Country iran = Country.createSanctioned("IR", "Iran", "Middle East");
        Company company = Company.create("Iranian Co", "REG-IR", iran);
        Vessel vessel = Vessel.builder()
                .imo(Imo.of("1234567"))
                .mmsi(Mmsi.of("123456789"))
                .name("IRAN VESSEL")
                .flag(Flag.of("IR"))
                .type(VesselType.TANKER)
                .company(company)
                .build();

        when(vesselRepository.findByImo("1234567")).thenReturn(Optional.of(vessel));
        Sanction sanction = Sanction.create("REF-001", "IR", "COUNTRY",
                "UN", LocalDate.now().minusYears(5), null, "Iran sanctions");
        when(sanctionRepository.findActiveSanctions()).thenReturn(List.of(sanction));

        // When
        boolean result = screeningService.isVesselSanctioned("1234567");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("비제재국 소유 선박이면 제재 대상이 아니다")
    void should_returnFalse_when_vesselFromNonSanctionedCountry() {
        // Given
        Country korea = Country.create("KR", "South Korea");
        Company company = Company.create("Korean Co", "REG-KR", korea);
        Vessel vessel = Vessel.builder()
                .imo(Imo.of("1234567"))
                .mmsi(Mmsi.of("123456789"))
                .name("KOREAN VESSEL")
                .flag(Flag.of("KR"))
                .type(VesselType.CONTAINER)
                .company(company)
                .build();

        when(vesselRepository.findByImo("1234567")).thenReturn(Optional.of(vessel));
        Sanction sanction = Sanction.create("REF-001", "IR", "COUNTRY",
                "UN", LocalDate.now().minusYears(5), null, "Iran sanctions");
        when(sanctionRepository.findActiveSanctions()).thenReturn(List.of(sanction));

        // When
        boolean result = screeningService.isVesselSanctioned("1234567");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 선박이면 false를 반환한다")
    void should_returnFalse_when_vesselNotFound() {
        // Given
        when(vesselRepository.findByImo("9999999")).thenReturn(Optional.empty());

        // When
        boolean result = screeningService.isVesselSanctioned("9999999");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("제재 대상 회사명과 일치하면 true를 반환한다")
    void should_returnTrue_when_companySanctioned() {
        // Given
        Sanction sanction = Sanction.create("REF-001", "Evil Corp", "ENTITY",
                "UN", LocalDate.now().minusYears(1), null, "Entity sanction");
        when(sanctionRepository.findActiveSanctions()).thenReturn(List.of(sanction));

        // When
        boolean result = screeningService.isCompanySanctioned("Evil Corp");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("제재 대상 회사명과 일치하지 않으면 false를 반환한다")
    void should_returnFalse_when_companyNotSanctioned() {
        // Given
        Sanction sanction = Sanction.create("REF-001", "Evil Corp", "ENTITY",
                "UN", LocalDate.now().minusYears(1), null, "Entity sanction");
        when(sanctionRepository.findActiveSanctions()).thenReturn(List.of(sanction));

        // When
        boolean result = screeningService.isCompanySanctioned("Good Corp");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("제재국 코드 목록을 반환한다")
    void should_returnSanctionedCountryCodes_when_called() {
        // Given
        Sanction s1 = Sanction.create("REF-001", "IR", "COUNTRY", "UN",
                LocalDate.now().minusYears(5), null, "Iran");
        Sanction s2 = Sanction.create("REF-002", "KP", "COUNTRY", "UN",
                LocalDate.now().minusYears(10), null, "North Korea");
        when(sanctionRepository.findActiveSanctions()).thenReturn(List.of(s1, s2));

        // When
        List<String> codes = screeningService.getSanctionedCountryCodes();

        // Then
        assertThat(codes).containsExactlyInAnyOrder("IR", "KP");
    }
}
