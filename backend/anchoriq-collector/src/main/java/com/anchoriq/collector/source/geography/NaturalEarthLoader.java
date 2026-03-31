package com.anchoriq.collector.source.geography;

import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Natural Earth / OSM 초크포인트 좌표 로더.
 * 6개 주요 초크포인트 데이터를 DB에 로드한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class NaturalEarthLoader implements GeographyLoader {

    private static final Logger log = LoggerFactory.getLogger(NaturalEarthLoader.class);

    private final ChokepointRepository chokepointRepository;

    public NaturalEarthLoader(ChokepointRepository chokepointRepository) {
        this.chokepointRepository = chokepointRepository;
    }

    @Override
    public void load() {
        log.info("Loading chokepoint data");

        List<ChokepointSeed> seeds = getChokepointSeeds();
        int loaded = 0;
        for (ChokepointSeed seed : seeds) {
            if (chokepointRepository.findByName(seed.name).isEmpty()) {
                Chokepoint cp = Chokepoint.create(
                        seed.name, seed.displayName, seed.lat, seed.lon,
                        seed.riskLevel, seed.description);
                chokepointRepository.save(cp);
                loaded++;
            }
        }
        log.info("Chokepoint data loading completed: {} new entries loaded", loaded);
    }

    @Override
    public String loaderName() {
        return "NaturalEarth";
    }

    private List<ChokepointSeed> getChokepointSeeds() {
        return List.of(
                new ChokepointSeed("Hormuz", "Strait of Hormuz",
                        26.5667, 56.25, "HIGH",
                        "21M barrels/day oil transit, Iran-Oman border"),
                new ChokepointSeed("Malacca", "Strait of Malacca",
                        2.5, 101.0, "MEDIUM",
                        "25% of global trade, Singapore-Malaysia-Indonesia border"),
                new ChokepointSeed("Bab-el-Mandeb", "Bab el-Mandeb",
                        12.5833, 43.3333, "HIGH",
                        "Red Sea entrance, Yemen-Djibouti border, Houthi attacks"),
                new ChokepointSeed("Suez", "Suez Canal",
                        30.4583, 32.3500, "MEDIUM",
                        "12% of global trade, Egypt, 193km artificial canal"),
                new ChokepointSeed("Taiwan", "Taiwan Strait",
                        24.0, 119.5, "HIGH",
                        "US-China tension, semiconductor supply chain, 180km width"),
                new ChokepointSeed("Panama", "Panama Canal",
                        9.0, -79.5, "MEDIUM",
                        "5% of global trade, drought restricting transit, 80km")
        );
    }

    private record ChokepointSeed(String name, String displayName, double lat, double lon,
                                   String riskLevel, String description) {
    }
}
