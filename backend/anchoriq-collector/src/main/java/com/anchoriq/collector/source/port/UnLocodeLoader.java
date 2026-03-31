package com.anchoriq.collector.source.port;

import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * UN/LOCODE 항만 정보 벌크 로더.
 * 항만 코드, 이름, 좌표 데이터를 DB에 로드한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class UnLocodeLoader {

    private static final Logger log = LoggerFactory.getLogger(UnLocodeLoader.class);
    private static final String LOCODE_API_URL = "https://unece.org/trade/cefact/unlocode-code-list-country-and-territory";

    private final PortRepository portRepository;
    private final WebClient webClient;

    public UnLocodeLoader(PortRepository portRepository, WebClient.Builder webClientBuilder) {
        this.portRepository = portRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * 주요 아시아 발착 항만 데이터를 로드한다.
     * 실제로는 UN/LOCODE CSV를 파싱하지만, 초기 시드 데이터로 주요 항만을 직접 등록한다.
     */
    public void loadInitialPorts() {
        log.info("Loading initial port data");

        List<PortSeed> majorPorts = getMajorAsianPorts();
        int loaded = 0;
        for (PortSeed seed : majorPorts) {
            if (!portRepository.existsByLocode(seed.locode)) {
                Port port = Port.create(seed.locode, seed.name, seed.country, seed.lat, seed.lon);
                portRepository.save(port);
                loaded++;
            }
        }
        log.info("Port data loading completed: {} new ports loaded out of {} total", loaded, majorPorts.size());
    }

    private List<PortSeed> getMajorAsianPorts() {
        return List.of(
                new PortSeed("KRPUS", "Busan", "KR", 35.1028, 129.0403),
                new PortSeed("KRINC", "Incheon", "KR", 37.4563, 126.7052),
                new PortSeed("CNSHA", "Shanghai", "CN", 31.2304, 121.4737),
                new PortSeed("CNSHE", "Shenzhen", "CN", 22.5431, 114.0579),
                new PortSeed("CNNGB", "Ningbo", "CN", 29.8683, 121.5440),
                new PortSeed("SGSIN", "Singapore", "SG", 1.2644, 103.8222),
                new PortSeed("JPYOK", "Yokohama", "JP", 35.4437, 139.6380),
                new PortSeed("JPTYO", "Tokyo", "JP", 35.6528, 139.8395),
                new PortSeed("JPKOB", "Kobe", "JP", 34.6901, 135.1956),
                new PortSeed("TWKHH", "Kaohsiung", "TW", 22.6273, 120.3014),
                new PortSeed("HKHKG", "Hong Kong", "HK", 22.2855, 114.1577),
                new PortSeed("VNSGN", "Ho Chi Minh City", "VN", 10.7769, 106.7009),
                new PortSeed("MYPKG", "Port Klang", "MY", 3.0000, 101.4000),
                new PortSeed("THLCH", "Laem Chabang", "TH", 13.0827, 100.8850),
                new PortSeed("IDDPS", "Tanjung Priok", "ID", -6.1053, 106.8817),
                new PortSeed("PHMNL", "Manila", "PH", 14.5995, 120.9842),
                new PortSeed("AEJEA", "Jebel Ali", "AE", 25.0000, 55.0600),
                new PortSeed("SAJED", "Jeddah", "SA", 21.4858, 39.1925),
                new PortSeed("EGPSD", "Port Said", "EG", 31.2565, 32.2841),
                new PortSeed("LKCMB", "Colombo", "LK", 6.9271, 79.8612)
        );
    }

    private record PortSeed(String locode, String name, String country, double lat, double lon) {
    }
}
