package com.anchoriq.core.domain.maritime.company.model;

import com.anchoriq.core.domain.maritime.country.model.Country;

import java.util.Objects;
import java.util.Set;

/**
 * 선박 소유 회사 엔티티.
 * 제재국 등록 여부 판단 로직을 보유한다.
 * 순수 POJO -- Neo4j 어노테이션 없음.
 */
public class Company {

    private Long id;
    private String name;
    private String registrationNumber;
    private Country country;

    protected Company() {
    }

    private Company(String name, String registrationNumber, Country country) {
        this.name = Objects.requireNonNull(name, "Company name must not be null");
        this.registrationNumber = registrationNumber;
        this.country = country;
    }

    public static Company create(String name) {
        return new Company(name, null, null);
    }

    public static Company create(String name, String registrationNumber) {
        return new Company(name, registrationNumber, null);
    }

    public static Company create(String name, String registrationNumber, Country country) {
        return new Company(name, registrationNumber, country);
    }

    public boolean isRegisteredInCountries(Set<String> countryCodes) {
        if (country == null) {
            return false;
        }
        return countryCodes.contains(country.getIsoCodeValue());
    }

    public void registerIn(Country country) {
        this.country = Objects.requireNonNull(country);
    }

    public static Company reconstitute(Long id, String name, String registrationNumber,
                                        Country country) {
        Company company = new Company(name, registrationNumber, country);
        company.id = id;
        return company;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public Country getCountry() {
        return country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return name.equals(company.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Company{name='%s'}", name);
    }
}
