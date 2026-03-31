package com.anchoriq.core.domain.maritime.country.model;

import java.util.Objects;

/**
 * 국가 엔티티.
 * 제재 여부 판단 로직을 보유한다.
 * 순수 POJO -- Neo4j 어노테이션 없음.
 */
public class Country {

    private Long id;
    private IsoCountryCode isoCode;
    private String name;
    private String region;
    private boolean sanctioned;

    protected Country() {
    }

    private Country(IsoCountryCode isoCode, String name, String region, boolean sanctioned) {
        this.isoCode = Objects.requireNonNull(isoCode, "ISO code must not be null");
        this.name = Objects.requireNonNull(name, "Country name must not be null");
        this.region = region;
        this.sanctioned = sanctioned;
    }

    public static Country create(String isoCode, String name) {
        return new Country(IsoCountryCode.of(isoCode), name, null, false);
    }

    public static Country create(String isoCode, String name, String region) {
        return new Country(IsoCountryCode.of(isoCode), name, region, false);
    }

    public static Country createSanctioned(String isoCode, String name, String region) {
        return new Country(IsoCountryCode.of(isoCode), name, region, true);
    }

    public void markSanctioned() {
        this.sanctioned = true;
    }

    public void removeSanction() {
        this.sanctioned = false;
    }

    public boolean isSanctioned() {
        return sanctioned;
    }

    public static Country reconstitute(Long id, IsoCountryCode isoCode, String name,
                                        String region, boolean sanctioned) {
        Country country = new Country(isoCode, name, region, sanctioned);
        country.id = id;
        return country;
    }

    public Long getId() {
        return id;
    }

    public IsoCountryCode getIsoCode() {
        return isoCode;
    }

    public String getIsoCodeValue() {
        return isoCode != null ? isoCode.value() : null;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Country country = (Country) o;
        return isoCode.equals(country.isoCode);
    }

    @Override
    public int hashCode() {
        return isoCode.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Country{iso='%s', name='%s', sanctioned=%s}", isoCode, name, sanctioned);
    }
}
