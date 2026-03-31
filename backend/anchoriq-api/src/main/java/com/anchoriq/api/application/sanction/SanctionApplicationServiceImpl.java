package com.anchoriq.api.application.sanction;

import com.anchoriq.api.dto.response.sanction.SanctionResponse;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 제재 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class SanctionApplicationServiceImpl implements SanctionApplicationService {

    private final SanctionRepository sanctionRepository;

    @Override
    public List<SanctionResponse> findAll(int page, int size) {
        List<Sanction> sanctions = sanctionRepository.findAll();
        int fromIndex = Math.min(page * size, sanctions.size());
        int toIndex = Math.min(fromIndex + size, sanctions.size());
        return sanctions.subList(fromIndex, toIndex).stream()
                .map(SanctionResponse::from)
                .toList();
    }

    @Override
    public SanctionResponse findById(Long id) {
        Sanction sanction = sanctionRepository.findAll().stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Sanction", id.toString()));
        return SanctionResponse.from(sanction);
    }

    @Override
    public List<SanctionResponse> search(String query) {
        return sanctionRepository.findByTargetNameContaining(query).stream()
                .map(SanctionResponse::from)
                .toList();
    }
}
