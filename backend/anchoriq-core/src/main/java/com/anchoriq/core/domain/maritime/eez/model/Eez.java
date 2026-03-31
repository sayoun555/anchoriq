package com.anchoriq.core.domain.maritime.eez.model;

import com.anchoriq.core.domain.maritime.country.model.IsoCountryCode;

import java.util.Objects;

/**
 * 배타적경제수역(EEZ) 엔티티.
 * Marine Regions GeoJSON 데이터에서 로드한다.
 * 순수 POJO -- Neo4j 어노테이션 없음.
 */
public class Eez {

    private Long id;
    private String name;
    private String country;
    private IsoCountryCode isoCode;
    private double areaKm2;

    protected Eez() {
    }

    private Eez(String name, String country, IsoCountryCode isoCode, double areaKm2) {
        this.name = Objects.requireNonNull(name, "EEZ name must not be null");
        this.country = country;
        this.isoCode = isoCode;
        this.areaKm2 = areaKm2;
    }

    public static Eez create(String name, String country, String isoCode, double areaKm2) {
        IsoCountryCode code = (isoCode != null && !isoCode.isBlank()) ? IsoCountryCode.of(isoCode) : null;
        return new Eez(name, country, code, areaKm2);
    }

    public boolean belongsToCountry(String countryCode) {
        return isoCode != null && isoCode.value().equalsIgnoreCase(countryCode);
    }

    public static Eez reconstitute(Long id, String name, String country,
                                    IsoCountryCode isoCode, double areaKm2) {
        Eez eez = new Eez(name, country, isoCode, areaKm2);
        eez.id = id;
        return eez;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public IsoCountryCode getIsoCode() {
        return isoCode;
    }

    public String getIsoCodeValue() {
        return isoCode != null ? isoCode.value() : null;
    }

    public double getAreaKm2() {
        return areaKm2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Eez eez = (Eez) o;
        return name.equals(eez.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Eez{name='%s', country='%s'}", name, country);
    }
}
