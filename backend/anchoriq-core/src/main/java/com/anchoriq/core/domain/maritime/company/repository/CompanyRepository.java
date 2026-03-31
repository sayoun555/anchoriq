package com.anchoriq.core.domain.maritime.company.repository;

import com.anchoriq.core.domain.maritime.company.model.Company;

import java.util.List;
import java.util.Optional;

/**
 * Company Repository 인터페이스.
 */
public interface CompanyRepository {

    Optional<Company> findByName(String name);

    List<Company> findAll();

    Company save(Company company);

    boolean existsByName(String name);

    long count();
}
