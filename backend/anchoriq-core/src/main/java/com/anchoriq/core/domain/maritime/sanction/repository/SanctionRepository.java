package com.anchoriq.core.domain.maritime.sanction.repository;

import com.anchoriq.core.domain.maritime.sanction.model.Sanction;

import java.util.List;
import java.util.Optional;

/**
 * Sanction Repository 인터페이스.
 */
public interface SanctionRepository {

    Optional<Sanction> findByReferenceNumber(String referenceNumber);

    List<Sanction> findAll();

    List<Sanction> findActiveSanctions();

    List<Sanction> findByTargetNameContaining(String query);

    Sanction save(Sanction sanction);

    long count();
}
