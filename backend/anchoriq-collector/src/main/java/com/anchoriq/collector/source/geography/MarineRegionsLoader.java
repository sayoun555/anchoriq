package com.anchoriq.collector.source.geography;

import com.anchoriq.core.domain.maritime.eez.model.Eez;
import com.anchoriq.core.domain.maritime.eez.repository.EezRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Marine Regions EEZ GeoJSON 로더.
 * 주요 아시아 해역의 EEZ 데이터를 DB에 로드한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class MarineRegionsLoader implements GeographyLoader {

    private static final Logger log = LoggerFactory.getLogger(MarineRegionsLoader.class);

    private final EezRepository eezRepository;

    public MarineRegionsLoader(EezRepository eezRepository) {
        this.eezRepository = eezRepository;
    }

    @Override
    public void load() {
        log.info("Loading EEZ data from Marine Regions");

        List<EezSeed> seeds = getMajorEezData();
        int loaded = 0;
        for (EezSeed seed : seeds) {
            if (eezRepository.findByName(seed.name).isEmpty()) {
                Eez eez = Eez.create(seed.name, seed.country, seed.isoCode, seed.areaKm2);
                eezRepository.save(eez);
                loaded++;
            }
        }
        log.info("EEZ data loading completed: {} new entries loaded", loaded);
    }

    @Override
    public String loaderName() {
        return "MarineRegions";
    }

    private List<EezSeed> getMajorEezData() {
        return List.of(
                new EezSeed("South Korean EEZ", "South Korea", "KR", 475000),
                new EezSeed("Japanese EEZ", "Japan", "JP", 4470000),
                new EezSeed("Chinese EEZ", "China", "CN", 877000),
                new EezSeed("Philippine EEZ", "Philippines", "PH", 2263816),
                new EezSeed("Vietnamese EEZ", "Vietnam", "VN", 1395000),
                new EezSeed("Indonesian EEZ", "Indonesia", "ID", 6159032),
                new EezSeed("Malaysian EEZ", "Malaysia", "MY", 453186),
                new EezSeed("Singaporean EEZ", "Singapore", "SG", 1067),
                new EezSeed("Thai EEZ", "Thailand", "TH", 307600),
                new EezSeed("Indian EEZ", "India", "IN", 2305143),
                new EezSeed("Saudi Arabian EEZ", "Saudi Arabia", "SA", 225000),
                new EezSeed("Iranian EEZ", "Iran", "IR", 168718),
                new EezSeed("Egyptian EEZ", "Egypt", "EG", 263451)
        );
    }

    private record EezSeed(String name, String country, String isoCode, double areaKm2) {
    }
}
