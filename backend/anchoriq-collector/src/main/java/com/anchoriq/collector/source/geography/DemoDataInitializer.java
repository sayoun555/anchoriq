package com.anchoriq.collector.source.geography;

import com.anchoriq.core.domain.maritime.company.model.Company;
import com.anchoriq.core.domain.maritime.company.repository.CompanyRepository;
import com.anchoriq.core.domain.maritime.country.model.Country;
import com.anchoriq.core.domain.maritime.country.repository.CountryRepository;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.model.VesselStatus;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 데모 데이터 초기화.
 * 국가, 회사, 선박을 Neo4j에 시드하고 항만 혼잡도를 업데이트한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class DemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final CountryRepository countryRepository;
    private final CompanyRepository companyRepository;
    private final VesselRepository vesselRepository;
    private final PortRepository portRepository;
    private final StringRedisTemplate redisTemplate;

    public DemoDataInitializer(CountryRepository countryRepository,
                               CompanyRepository companyRepository,
                               VesselRepository vesselRepository,
                               PortRepository portRepository,
                               StringRedisTemplate redisTemplate) {
        this.countryRepository = countryRepository;
        this.companyRepository = companyRepository;
        this.vesselRepository = vesselRepository;
        this.portRepository = portRepository;
        this.redisTemplate = redisTemplate;
    }

    public void loadDemoData() {
        log.info("Loading demo data...");
        int countriesLoaded = loadCountries();
        int companiesLoaded = loadCompanies();
        int vesselsLoaded = loadVessels();
        int portsUpdated = updatePortCongestion();
        int positionsSeeded = seedRedisGeoPositions();
        log.info("Demo data loading completed: {} countries, {} companies, {} vessels, {} ports updated, {} Redis GEO positions",
                countriesLoaded, companiesLoaded, vesselsLoaded, portsUpdated, positionsSeeded);
    }

    private int loadCountries() {
        List<CountrySeed> seeds = List.of(
                new CountrySeed("KR", "South Korea", "East Asia", false),
                new CountrySeed("CN", "China", "East Asia", false),
                new CountrySeed("SG", "Singapore", "Southeast Asia", false),
                new CountrySeed("JP", "Japan", "East Asia", false),
                new CountrySeed("US", "United States", "North America", false),
                new CountrySeed("DE", "Germany", "Europe", false),
                new CountrySeed("GR", "Greece", "Europe", false),
                new CountrySeed("PA", "Panama", "Central America", false),
                new CountrySeed("LR", "Liberia", "West Africa", false),
                new CountrySeed("IR", "Iran", "Middle East", true),
                new CountrySeed("KP", "North Korea", "East Asia", true)
        );

        int loaded = 0;
        for (CountrySeed seed : seeds) {
            if (countryRepository.findByIsoCode(seed.isoCode).isEmpty()) {
                Country country = seed.sanctioned
                        ? Country.createSanctioned(seed.isoCode, seed.name, seed.region)
                        : Country.create(seed.isoCode, seed.name, seed.region);
                countryRepository.save(country);
                loaded++;
            }
        }
        return loaded;
    }

    private int loadCompanies() {
        List<CompanySeed> seeds = List.of(
                new CompanySeed("Maersk", "DK-12345678", "DE"),
                new CompanySeed("COSCO Shipping", "CN-91001234", "CN"),
                new CompanySeed("Evergreen Marine", "TW-98765432", "SG"),
                new CompanySeed("HMM", "KR-20190001", "KR"),
                new CompanySeed("Ocean Network Express", "JP-30001234", "JP"),
                new CompanySeed("IRISL Group", "IR-10001000", "IR")
        );

        int loaded = 0;
        for (CompanySeed seed : seeds) {
            if (companyRepository.findByName(seed.name).isEmpty()) {
                Company company = Company.create(seed.name, seed.registrationNumber);
                countryRepository.findByIsoCode(seed.countryCode)
                        .ifPresent(company::registerIn);
                companyRepository.save(company);
                loaded++;
            }
        }
        return loaded;
    }

    private int loadVessels() {
        Set<String> sanctionedCodes = Set.of("IR", "KP", "SY", "CU");
        Set<String> highRiskFlags = Set.of("IR", "KP", "CM", "TZ");

        List<VesselSeed> seeds = List.of(
                // Maersk fleet
                new VesselSeed("9778791", "219018000", "MSC GULSUN", "PA", VesselType.CONTAINER, 228000, 2019, "Maersk", VesselStatus.SAILING),
                new VesselSeed("9839430", "219019000", "MAERSK MCKINNEY", "PA", VesselType.CONTAINER, 210000, 2021, "Maersk", VesselStatus.SAILING),
                new VesselSeed("9619907", "219020000", "MAERSK HONAM", "SG", VesselType.CONTAINER, 186000, 2017, "Maersk", VesselStatus.SAILING),
                // COSCO fleet
                new VesselSeed("9785400", "477100000", "COSCO UNIVERSE", "CN", VesselType.CONTAINER, 198000, 2018, "COSCO Shipping", VesselStatus.SAILING),
                new VesselSeed("9461900", "477200000", "COSCO PRIDE", "CN", VesselType.BULK_CARRIER, 180000, 2011, "COSCO Shipping", VesselStatus.ANCHORED),
                new VesselSeed("9300800", "477300000", "COSCO DALIAN", "CN", VesselType.TANKER, 160000, 2006, "COSCO Shipping", VesselStatus.SAILING),
                // Evergreen fleet
                new VesselSeed("9811000", "416801000", "EVER ACE", "PA", VesselType.CONTAINER, 235000, 2021, "Evergreen Marine", VesselStatus.SAILING),
                new VesselSeed("9629080", "416802000", "EVER GIVEN", "PA", VesselType.CONTAINER, 220000, 2018, "Evergreen Marine", VesselStatus.MOORED),
                // HMM fleet
                new VesselSeed("9863297", "440101000", "HMM ALGECIRAS", "KR", VesselType.CONTAINER, 228000, 2020, "HMM", VesselStatus.SAILING),
                new VesselSeed("9869538", "440102000", "HMM COPENHAGEN", "KR", VesselType.CONTAINER, 228000, 2020, "HMM", VesselStatus.SAILING),
                new VesselSeed("9500432", "440103000", "HMM DREAM", "KR", VesselType.BULK_CARRIER, 150000, 2013, "HMM", VesselStatus.ANCHORED),
                // ONE fleet
                new VesselSeed("9806043", "431001000", "ONE APUS", "JP", VesselType.CONTAINER, 197000, 2019, "Ocean Network Express", VesselStatus.SAILING),
                new VesselSeed("9302152", "431002000", "ONE COLUMBA", "LR", VesselType.CONTAINER, 140000, 2005, "Ocean Network Express", VesselStatus.SAILING),
                // Tankers
                new VesselSeed("9722000", "636092000", "YUAN SHAN HU", "LR", VesselType.TANKER, 320000, 2016, "COSCO Shipping", VesselStatus.SAILING),
                new VesselSeed("9401234", "636093000", "FRONT ALTA", "LR", VesselType.TANKER, 300000, 2010, "Maersk", VesselStatus.SAILING),
                // LNG Carriers
                new VesselSeed("9850001", "440201000", "SK SERENITY", "KR", VesselType.LNG_CARRIER, 93000, 2022, "HMM", VesselStatus.SAILING),
                new VesselSeed("9850002", "431201000", "PACIFIC BREEZE", "JP", VesselType.LNG_CARRIER, 87000, 2020, "Ocean Network Express", VesselStatus.SAILING),
                // Bulk Carriers
                new VesselSeed("9600123", "538005000", "CAPE VICTORIA", "LR", VesselType.BULK_CARRIER, 180000, 2004, "Maersk", VesselStatus.SAILING),
                new VesselSeed("9600124", "477400000", "GREAT WALL", "CN", VesselType.BULK_CARRIER, 170000, 2003, "COSCO Shipping", VesselStatus.ANCHORED),
                // IRISL (Iran — high risk)
                new VesselSeed("9167253", "422001000", "SHAHR-E-KORD", "IR", VesselType.CONTAINER, 42000, 2000, "IRISL Group", VesselStatus.SAILING),
                new VesselSeed("9209324", "422002000", "IRAN HORMUZ 20", "IR", VesselType.TANKER, 70000, 1999, "IRISL Group", VesselStatus.UNKNOWN),
                new VesselSeed("9283019", "422003000", "SABALAN", "IR", VesselType.BULK_CARRIER, 55000, 2004, "IRISL Group", VesselStatus.SAILING),
                // Old high-risk vessels
                new VesselSeed("9100001", "538090000", "OCEAN TRADER", "LR", VesselType.TANKER, 95000, 2001, null, VesselStatus.NOT_UNDER_COMMAND),
                new VesselSeed("9100002", "538091000", "SEA PHANTOM", "PA", VesselType.BULK_CARRIER, 80000, 1998, null, VesselStatus.UNKNOWN),
                new VesselSeed("9100003", "636099000", "DARK NAVIGATOR", "LR", VesselType.TANKER, 110000, 2003, null, VesselStatus.SAILING)
        );

        int loaded = 0;
        for (VesselSeed seed : seeds) {
            if (!vesselRepository.existsByImo(seed.imo)) {
                Vessel vessel = Vessel.builder()
                        .imo(com.anchoriq.core.domain.maritime.vessel.model.Imo.of(seed.imo))
                        .mmsi(com.anchoriq.core.domain.maritime.vessel.model.Mmsi.of(seed.mmsi))
                        .name(seed.name)
                        .flag(com.anchoriq.core.domain.maritime.vessel.model.Flag.of(seed.flag))
                        .type(seed.type)
                        .status(seed.status)
                        .deadweight(seed.deadweight)
                        .buildYear(seed.buildYear)
                        .build();

                if (seed.companyName != null) {
                    companyRepository.findByName(seed.companyName)
                            .ifPresent(vessel::assignCompany);
                }

                vessel.evaluateRiskScore(sanctionedCodes, highRiskFlags);
                vesselRepository.save(vessel);
                loaded++;
            }
        }
        return loaded;
    }

    private int updatePortCongestion() {
        List<PortCongestionSeed> seeds = List.of(
                new PortCongestionSeed("SGSIN", 85.0, 42),
                new PortCongestionSeed("CNSHA", 78.0, 38),
                new PortCongestionSeed("KRPUS", 52.0, 22),
                new PortCongestionSeed("CNNGB", 72.0, 34),
                new PortCongestionSeed("HKHKG", 68.0, 30),
                new PortCongestionSeed("JPYOK", 35.0, 15),
                new PortCongestionSeed("TWKHH", 45.0, 18),
                new PortCongestionSeed("VNHPH", 55.0, 20),
                new PortCongestionSeed("THLCH", 42.0, 16),
                new PortCongestionSeed("PHMNL", 38.0, 14),
                new PortCongestionSeed("MYPKG", 60.0, 24),
                new PortCongestionSeed("IDTPP", 48.0, 19),
                new PortCongestionSeed("JPNGO", 30.0, 12),
                new PortCongestionSeed("JPTYO", 33.0, 13),
                new PortCongestionSeed("CNQDG", 65.0, 28),
                new PortCongestionSeed("CNTXG", 70.0, 32),
                new PortCongestionSeed("CNSZX", 75.0, 35),
                new PortCongestionSeed("INMAA", 40.0, 15),
                new PortCongestionSeed("LKCMB", 32.0, 11),
                new PortCongestionSeed("BDCGP", 58.0, 21)
        );

        int updated = 0;
        for (PortCongestionSeed seed : seeds) {
            portRepository.findByLocode(seed.locode).ifPresent(port -> {
                port.updateCongestion(seed.congestionLevel);
                for (int i = 0; i < seed.vesselCount; i++) {
                    port.acceptVessel();
                }
                portRepository.save(port);
            });
            updated++;
        }
        return updated;
    }

    private static final double[][] VESSEL_POSITIONS = {
            {1.28, 103.85},   // Singapore Strait
            {31.23, 121.47},  // Shanghai
            {35.10, 129.04},  // Busan
            {22.30, 114.17},  // Hong Kong
            {34.67, 135.43},  // Osaka
            {3.50, 99.50},    // Malacca Strait
            {26.50, 56.25},   // Hormuz
            {30.45, 32.35},   // Suez
            {12.58, 43.33},   // Bab-el-Mandeb
            {24.00, 119.50},  // Taiwan Strait
            {9.00, -79.50},   // Panama
            {35.65, 139.75},  // Tokyo Bay
            {14.58, 120.97},  // Manila
            {10.30, 107.08},  // Ho Chi Minh
            {13.10, 100.90},  // Laem Chabang
            {2.95, 104.15},   // South China Sea
            {5.30, 100.35},   // Penang
            {37.45, 126.60},  // Incheon
            {25.26, 55.30},   // Dubai
            {33.34, 131.60},  // Kitakyushu
            {29.87, 121.55},  // Ningbo
            {30.70, 122.10},  // East China Sea
            {7.00, 80.00},    // Sri Lanka
            {20.00, 110.00},  // Hainan
            {36.00, 120.38},  // Qingdao
    };

    private int seedRedisGeoPositions() {
        try {
            var vessels = vesselRepository.findAll();
            int seeded = 0;
            String now = Instant.now().toString();

            for (int i = 0; i < vessels.size(); i++) {
                var vessel = vessels.get(i);
                String mmsi = vessel.getMmsi() != null ? vessel.getMmsi().value() : null;
                if (mmsi == null) continue;

                double[] pos = VESSEL_POSITIONS[i % VESSEL_POSITIONS.length];
                double lat = pos[0] + (i * 0.01);
                double lon = pos[1] + (i * 0.01);

                redisTemplate.opsForGeo().add("vessels:positions", new Point(lon, lat), mmsi);
                redisTemplate.opsForValue().set("vessels:timestamp:" + mmsi, now, Duration.ofHours(24));

                String status = vessel.getStatus() != null ? vessel.getStatus().name() : "UNKNOWN";
                redisTemplate.opsForValue().set("vessels:status:" + mmsi, status, Duration.ofHours(24));

                double heading = 90.0 + (i * 15.0) % 360;
                double speed = vessel.getStatus() == VesselStatus.SAILING ? 12.0 + (i % 8) : 0.0;
                redisTemplate.opsForValue().set("vessels:heading:" + mmsi, String.valueOf(heading), Duration.ofHours(24));
                redisTemplate.opsForValue().set("vessels:speed:" + mmsi, String.valueOf(speed), Duration.ofHours(24));

                seeded++;
            }
            return seeded;
        } catch (Exception e) {
            log.warn("Failed to seed Redis GEO positions: {}", e.getMessage());
            return 0;
        }
    }

    private record CountrySeed(String isoCode, String name, String region, boolean sanctioned) {}
    private record CompanySeed(String name, String registrationNumber, String countryCode) {}
    private record VesselSeed(String imo, String mmsi, String name, String flag,
                              VesselType type, int deadweight, int buildYear,
                              String companyName, VesselStatus status) {}
    private record PortCongestionSeed(String locode, double congestionLevel, int vesselCount) {}
}
