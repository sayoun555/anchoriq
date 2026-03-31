package com.anchoriq.api.controller.risk;

import com.anchoriq.api.application.risk.RiskScoreApplicationService;
import com.anchoriq.api.infrastructure.security.JwtTokenProvider;
import com.anchoriq.api.infrastructure.security.SecurityConfig;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.intelligence.risk.model.RiskType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskScoreController.class)
@Import(SecurityConfig.class)
@DisplayName("RiskScoreController API 엔드포인트 테스트")
class RiskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskScoreApplicationService riskScoreApplicationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    @DisplayName("선박 리스크 스코어 조회 시 200 OK와 스코어를 반환한다")
    void should_return200WithScore_when_vesselRiskScoreRequested() throws Exception {
        // Given
        RiskScore score = RiskScore.of("1234567", "VESSEL", 65,
                Map.of(RiskType.SANCTION, 40, RiskType.WEATHER, 25),
                "Vessel is registered in a sanctioned country; Severe weather detected");
        when(riskScoreApplicationService.getVesselRiskScore("1234567")).thenReturn(score);

        // When & Then
        mockMvc.perform(get("/api/risk/score/vessel/1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetId").value("1234567"))
                .andExpect(jsonPath("$.data.targetType").value("VESSEL"))
                .andExpect(jsonPath("$.data.score").value(65))
                .andExpect(jsonPath("$.data.level").value("HIGH"));
    }

    @Test
    @WithMockUser
    @DisplayName("항만 리스크 스코어 조회 시 200 OK를 반환한다")
    void should_return200_when_portRiskScoreRequested() throws Exception {
        // Given
        RiskScore score = RiskScore.of("KRPUS", "PORT", 30,
                Map.of(RiskType.CONGESTION, 30), "Moderate congestion");
        when(riskScoreApplicationService.getPortRiskScore("KRPUS")).thenReturn(score);

        // When & Then
        mockMvc.perform(get("/api/risk/score/port/KRPUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetId").value("KRPUS"))
                .andExpect(jsonPath("$.data.score").value(30))
                .andExpect(jsonPath("$.data.level").value("MEDIUM"));
    }

    @Test
    @WithMockUser
    @DisplayName("초크포인트 리스크 스코어 조회 시 200 OK를 반환한다")
    void should_return200_when_chokepointRiskScoreRequested() throws Exception {
        // Given
        RiskScore score = RiskScore.of("Hormuz", "CHOKEPOINT", 85,
                Map.of(RiskType.GEOPOLITICAL, 70, RiskType.CONGESTION, 40),
                "High geopolitical risk; High traffic volume");
        when(riskScoreApplicationService.getChokepointRiskScore("Hormuz")).thenReturn(score);

        // When & Then
        mockMvc.perform(get("/api/risk/score/chokepoint/Hormuz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetId").value("Hormuz"))
                .andExpect(jsonPath("$.data.score").value(85))
                .andExpect(jsonPath("$.data.level").value("CRITICAL"));
    }

    @Test
    @DisplayName("인증되지 않은 리스크 조회 요청은 401을 반환한다")
    void should_return401_when_unauthenticatedRiskRequest() throws Exception {
        mockMvc.perform(get("/api/risk/score/vessel/1234567"))
                .andExpect(status().isUnauthorized());
    }
}
