package com.anchoriq.api.controller.vessel;

import com.anchoriq.api.application.vessel.VesselQueryApplicationService;
import com.anchoriq.api.dto.response.vessel.VesselResponse;
import com.anchoriq.api.infrastructure.security.JwtTokenProvider;
import com.anchoriq.api.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VesselQueryController.class)
@Import(SecurityConfig.class)
@DisplayName("VesselQueryController API 엔드포인트 테스트")
class VesselControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VesselQueryApplicationService vesselQueryApplicationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("인증되지 않은 요청은 401을 반환한다")
    void should_return401_when_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/vessels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("인증된 사용자의 선박 목록 조회는 200 OK를 반환한다")
    void should_return200_when_authenticatedUserRequestsVessels() throws Exception {
        // Given
        VesselResponse vessel = VesselResponse.builder()
                .imo("1234567")
                .mmsi("123456789")
                .name("EVER GIVEN")
                .flag("PA")
                .type("CONTAINER")
                .status("SAILING")
                .deadweight(200000)
                .buildYear(2018)
                .age(8)
                .build();
        when(vesselQueryApplicationService.findAll()).thenReturn(List.of(vessel));

        // When & Then
        mockMvc.perform(get("/api/vessels")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].imo").value("1234567"))
                .andExpect(jsonPath("$.data.content[0].name").value("EVER GIVEN"));
    }

    @Test
    @WithMockUser
    @DisplayName("IMO로 선박 상세 조회 시 200 OK를 반환한다")
    void should_return200_when_vesselFoundByImo() throws Exception {
        // Given
        VesselResponse vessel = VesselResponse.builder()
                .imo("1234567")
                .name("EVER GIVEN")
                .flag("PA")
                .type("CONTAINER")
                .status("SAILING")
                .build();
        when(vesselQueryApplicationService.findByImo("1234567")).thenReturn(vessel);

        // When & Then
        mockMvc.perform(get("/api/vessels/1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imo").value("1234567"));
    }
}
