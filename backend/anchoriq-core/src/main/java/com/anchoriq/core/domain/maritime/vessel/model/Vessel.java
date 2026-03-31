package com.anchoriq.core.domain.maritime.vessel.model;

import com.anchoriq.core.domain.common.event.RiskScoreChangedEvent;
import com.anchoriq.core.domain.common.event.VesselStatusChangedEvent;
import com.anchoriq.core.domain.common.model.AggregateRoot;
import com.anchoriq.core.domain.maritime.company.model.Company;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.model.Route;

import java.time.Instant;
import java.time.Year;
import java.util.Objects;
import java.util.Set;

/**
 * 선박 엔티티 (Aggregate Root).
 * 온톨로지 중심 엔티티로 리스크 자체 판단 로직을 보유한다.
 * 순수 POJO — Neo4j 어노테이션 없음. 매핑은 infrastructure 레이어에서 수행.
 */
public class Vessel extends AggregateRoot {

    private Long id;
    private Imo imo;
    private Mmsi mmsi;
    private String name;
    private Flag flag;
    private VesselType type;
    private VesselStatus status;
    private int deadweight;
    private int buildYear;
    private Instant lastUpdated;
    private Company company;
    private int riskScore;

    protected Vessel() {
    }

    private Vessel(Builder builder) {
        this.imo = builder.imo;
        this.mmsi = Objects.requireNonNull(builder.mmsi, "MMSI must not be null");
        this.name = Objects.requireNonNull(builder.name, "Name must not be null");
        this.flag = builder.flag;
        this.type = Objects.requireNonNull(builder.type, "Type must not be null");
        this.status = builder.status != null ? builder.status : VesselStatus.UNKNOWN;
        this.deadweight = builder.deadweight;
        this.buildYear = builder.buildYear;
        this.lastUpdated = Instant.now();
        this.company = builder.company;
        this.riskScore = 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Vessel create(String imo, String mmsi, String name, String flag,
                                VesselType type) {
        return builder()
                .imo(Imo.of(imo))
                .mmsi(Mmsi.of(mmsi))
                .name(name)
                .flag(Flag.of(flag))
                .type(type)
                .build();
    }

    public static Vessel create(String imo, String mmsi, String name, String flag,
                                VesselType type, int deadweight, int buildYear) {
        return builder()
                .imo(Imo.of(imo))
                .mmsi(Mmsi.of(mmsi))
                .name(name)
                .flag(Flag.of(flag))
                .type(type)
                .deadweight(deadweight)
                .buildYear(buildYear)
                .build();
    }

    // --- 상태 머신 ---

    /**
     * 선박 상태를 변경한다.
     * 유효하지 않은 전이 시 IllegalStateException을 던진다.
     * 상태 변경 시 VesselStatusChangedEvent를 발행한다.
     */
    public void changeStatus(VesselStatus newStatus) {
        Objects.requireNonNull(newStatus, "New status must not be null");
        this.status.validateTransitionTo(newStatus);
        String previousStatusName = this.status.name();
        this.status = newStatus;
        this.lastUpdated = Instant.now();
        String imoValue = this.imo != null ? this.imo.value() : this.mmsi.value();
        registerEvent(new VesselStatusChangedEvent(
                imoValue, previousStatusName, newStatus.name()));
    }

    /**
     * AIS 원시 데이터에서 상태를 직접 갱신한다.
     * AIS 수신기는 모든 상태를 전송하므로 전환 검증을 수행하지 않는다.
     */
    public void updateStatusFromAis(VesselStatus newStatus) {
        Objects.requireNonNull(newStatus, "New status must not be null");
        this.status = newStatus;
        this.lastUpdated = Instant.now();
    }

    // --- 비즈니스 로직 ---

    public boolean isRegisteredInSanctionedCountry(Set<String> sanctionedCountryCodes) {
        if (company == null) {
            return false;
        }
        return company.isRegisteredInCountries(sanctionedCountryCodes);
    }

    public boolean isHighRiskAtChokepoint(Chokepoint chokepoint,
                                          Set<String> sanctionedCountryCodes) {
        return isRegisteredInSanctionedCountry(sanctionedCountryCodes)
                && chokepoint.isHighRisk();
    }

    /**
     * 리스크 점수를 종합 평가한다 (0~100).
     * 국적, 선령, 타입, 제재 여부를 종합하여 계산.
     */
    public int evaluateRiskScore(Set<String> sanctionedCountryCodes,
                                 Set<String> highRiskFlags) {
        int previousScore = this.riskScore;
        int score = 0;

        // 제재국 연관: +40
        if (isRegisteredInSanctionedCountry(sanctionedCountryCodes)) {
            score += 40;
        }

        // 고위험 국기: +20
        if (highRiskFlags.contains(this.flag.value())) {
            score += 20;
        }

        // 선령 (20년 이상: +15, 15년 이상: +10)
        int age = calculateAge();
        if (age >= 20) {
            score += 15;
        } else if (age >= 15) {
            score += 10;
        }

        // 탱커는 추가 리스크: +10
        if (isTanker()) {
            score += 10;
        }

        // AIS 소실(NOT_UNDER_COMMAND 등 비정상 상태): +15
        if (this.status == VesselStatus.NOT_UNDER_COMMAND
                || this.status == VesselStatus.UNKNOWN) {
            score += 15;
        }

        this.riskScore = Math.min(score, 100);

        if (previousScore != this.riskScore) {
            registerEvent(new RiskScoreChangedEvent(
                    this.imo.value(), "VESSEL", previousScore, this.riskScore));
        }

        return this.riskScore;
    }

    /**
     * 항로를 변경한다.
     * 운항 중인 선박만 항로 변경이 가능하다.
     */
    public void changeRoute(Route newRoute) {
        Objects.requireNonNull(newRoute, "Route must not be null");
        if (this.status != VesselStatus.SAILING) {
            throw new IllegalStateException("Only sailing vessels can change route. Current status: " + this.status);
        }
        this.lastUpdated = Instant.now();
    }

    /**
     * AIS 소실을 신고한다.
     */
    public void reportAisOff() {
        this.lastUpdated = Instant.now();
    }

    /**
     * 제재국 연관 선박으로 표시한다.
     * SanctionedVesselDetectedEvent를 발행한다.
     */
    public void markAsSanctionedCountryVessel() {
        registerEvent(new com.anchoriq.core.domain.common.event.SanctionedVesselDetectedEvent(this.imo));
    }

    public void assignCompany(Company company) {
        this.company = Objects.requireNonNull(company);
        this.lastUpdated = Instant.now();
    }

    public int calculateAge() {
        if (buildYear <= 0) {
            return 0;
        }
        return Year.now().getValue() - buildYear;
    }

    public boolean isTanker() {
        return this.type == VesselType.TANKER;
    }

    public boolean isSailing() {
        return this.status == VesselStatus.SAILING;
    }

    // --- 재구성용 (infrastructure 레이어에서 DB 데이터로 객체 복원 시) ---

    public static Vessel reconstitute(Long id, Imo imo, Mmsi mmsi, String name,
                                       Flag flag, VesselType type, VesselStatus status,
                                       int deadweight, int buildYear, Company company,
                                       Instant lastUpdated, int riskScore) {
        Vessel vessel = new Vessel();
        vessel.id = id;
        vessel.imo = imo;
        vessel.mmsi = mmsi;
        vessel.name = name;
        vessel.flag = flag;
        vessel.type = type;
        vessel.status = status;
        vessel.deadweight = deadweight;
        vessel.buildYear = buildYear;
        vessel.company = company;
        vessel.lastUpdated = lastUpdated;
        vessel.riskScore = riskScore;
        return vessel;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Imo getImo() {
        return imo;
    }

    public Mmsi getMmsi() {
        return mmsi;
    }

    public String getName() {
        return name;
    }

    public Flag getFlag() {
        return flag;
    }

    public VesselType getType() {
        return type;
    }

    public VesselStatus getStatus() {
        return status;
    }

    public int getDeadweight() {
        return deadweight;
    }

    public int getBuildYear() {
        return buildYear;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public Company getCompany() {
        return company;
    }

    public int getRiskScore() {
        return riskScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vessel vessel = (Vessel) o;
        return imo.equals(vessel.imo);
    }

    @Override
    public int hashCode() {
        return imo.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Vessel{imo='%s', name='%s', flag='%s', type=%s, status=%s}",
                imo, name, flag, type, status);
    }

    // --- Builder ---

    public static class Builder {
        private Imo imo;
        private Mmsi mmsi;
        private String name;
        private Flag flag;
        private VesselType type;
        private VesselStatus status;
        private int deadweight;
        private int buildYear;
        private Company company;

        private Builder() {
        }

        public Builder imo(Imo imo) {
            this.imo = imo;
            return this;
        }

        public Builder mmsi(Mmsi mmsi) {
            this.mmsi = mmsi;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder flag(Flag flag) {
            this.flag = flag;
            return this;
        }

        public Builder type(VesselType type) {
            this.type = type;
            return this;
        }

        public Builder status(VesselStatus status) {
            this.status = status;
            return this;
        }

        public Builder deadweight(int deadweight) {
            this.deadweight = deadweight;
            return this;
        }

        public Builder buildYear(int buildYear) {
            this.buildYear = buildYear;
            return this;
        }

        public Builder company(Company company) {
            this.company = company;
            return this;
        }

        public Vessel build() {
            return new Vessel(this);
        }
    }
}
