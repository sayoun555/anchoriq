package com.anchoriq.api.application.sanction;

import com.anchoriq.api.dto.response.sanction.SanctionResponse;

import java.util.List;

/**
 * 제재 Application Service 인터페이스.
 */
public interface SanctionApplicationService {

    List<SanctionResponse> findAll(int page, int size);

    SanctionResponse findById(Long id);

    List<SanctionResponse> search(String query);
}
