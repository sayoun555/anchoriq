package com.anchoriq.core.domain.maritime.country.repository;

import com.anchoriq.core.domain.maritime.country.model.Country;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Country Repository 인터페이스.
 */
public interface CountryRepository {

    Optional<Country> findByIsoCode(String isoCode);

    List<Country> findAll();

    List<Country> findSanctionedCountries();

    Set<String> findSanctionedCountryCodes();

    Country save(Country country);

    boolean existsByIsoCode(String isoCode);

    long count();
}
