package com.anchoriq.api.application.vessel;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import com.anchoriq.api.dto.response.vessel.VesselResponse;
import com.anchoriq.api.dto.response.vessel.VesselStatisticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 선박 조회 Application Service 구현체.
 * 오케스트레이션만 수행하며 비즈니스 로직은 Entity에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VesselQueryApplicationServiceImpl implements VesselQueryApplicationService {

    private final VesselRepository vesselRepository;

    @Override
    public List<VesselResponse> findAll() {
        try {
            return vesselRepository.findAll().stream()
                    .map(VesselResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch all vessels: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<VesselResponse> findAll(int page, int size) {
        try {
            return vesselRepository.findAll(page, size).stream()
                    .map(VesselResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch vessels: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public VesselResponse findByImo(String imo) {
        Vessel vessel = vesselRepository.findByImo(imo)
                .orElseThrow(() -> new EntityNotFoundException("Vessel", imo));
        return VesselResponse.from(vessel);
    }

    @Override
    public List<VesselResponse> findByFlag(String flag) {
        return vesselRepository.findByFlag(flag).stream()
                .map(VesselResponse::from)
                .toList();
    }

    @Override
    public List<VesselResponse> findByType(VesselType type) {
        return vesselRepository.findByType(type).stream()
                .map(VesselResponse::from)
                .toList();
    }

    @Override
    public List<VesselResponse> findSanctionedVessels() {
        try {
            return vesselRepository.findSanctionedVessels().stream()
                    .map(VesselResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch sanctioned vessels: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public VesselStatisticsResponse getStatistics() {
        try {
            List<Vessel> allVessels = vesselRepository.findAll();

            Map<String, Long> byType = allVessels.stream()
                    .filter(v -> v.getType() != null)
                    .collect(Collectors.groupingBy(v -> v.getType().name(), Collectors.counting()));

            Map<String, Long> byFlag = allVessels.stream()
                    .filter(v -> v.getFlag() != null)
                    .collect(Collectors.groupingBy(v -> v.getFlag().value(), Collectors.counting()));

            long sanctionedCount = vesselRepository.findSanctionedVessels().size();

            return VesselStatisticsResponse.builder()
                    .totalVessels(allVessels.size())
                    .byType(byType)
                    .byFlag(byFlag)
                    .sanctionedCount(sanctionedCount)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch vessel statistics: {}", e.getMessage());
            return VesselStatisticsResponse.builder()
                    .totalVessels(0)
                    .byType(Map.of())
                    .byFlag(Map.of())
                    .sanctionedCount(0)
                    .build();
        }
    }

    @Override
    public List<VesselResponse> searchByName(String query) {
        try {
            return vesselRepository.searchByName(query).stream()
                    .map(VesselResponse::from)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to search vessels: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long count() {
        try {
            return vesselRepository.count();
        } catch (Exception e) {
            log.warn("Failed to count vessels: {}", e.getMessage());
            return 0;
        }
    }
}
