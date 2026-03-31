package com.anchoriq.collector.config;

import com.anchoriq.collector.producer.AisKafkaProducer;
import com.anchoriq.collector.producer.GeopoliticalKafkaProducer;
import com.anchoriq.collector.producer.MarketKafkaProducer;
import com.anchoriq.collector.producer.NewsKafkaProducer;
import com.anchoriq.collector.producer.PortCongestionKafkaProducer;
import com.anchoriq.collector.producer.SanctionKafkaProducer;
import com.anchoriq.collector.producer.WeatherKafkaProducer;
import com.anchoriq.collector.registry.CollectorAutoStartProperties;
import com.anchoriq.collector.registry.CollectorRegistryImpl;
import com.anchoriq.collector.source.ais.AisMessageParser;
import com.anchoriq.collector.source.ais.AisStreamClient;
import com.anchoriq.collector.source.ais.AisStreamWebSocketClient;
import com.anchoriq.collector.source.geopolitical.GdeltCollector;
import com.anchoriq.collector.source.geography.MarineRegionsLoader;
import com.anchoriq.collector.source.geography.NaturalEarthLoader;
import com.anchoriq.collector.source.market.EiaOilPriceCollector;
import com.anchoriq.collector.source.market.FrankfurterCollector;
import com.anchoriq.collector.source.news.GNewsCollector;
import com.anchoriq.collector.infrastructure.port.PortCongestionCalculatorImpl;
import com.anchoriq.collector.source.port.UncladStatisticsDownloader;
import com.anchoriq.collector.source.port.UnLocodeLoader;
import com.anchoriq.core.domain.maritime.port.service.PortCongestionCalculator;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import com.anchoriq.core.domain.operation.collector.service.CollectorRegistry;
import com.anchoriq.collector.source.sanction.OpenSanctionsCollector;
import com.anchoriq.collector.source.weather.OpenMeteoCollector;
import com.anchoriq.collector.source.geography.DemoDataInitializer;
import com.anchoriq.core.domain.maritime.company.repository.CompanyRepository;
import com.anchoriq.core.domain.maritime.country.repository.CountryRepository;
import com.anchoriq.core.domain.maritime.eez.repository.EezRepository;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Collector Bean 설정.
 * 각 데이터 수집기, Kafka Producer, 지리 데이터 로더를 @Bean으로 명시적 등록한다.
 * 구현체에서 @Component를 제거하고 순수 POJO로 유지한다.
 */
@Configuration
public class CollectorConfig {

    // --- Kafka Producers (Kafka가 활성화된 환경에서만 Bean 생성) ---

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public AisKafkaProducer aisKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new AisKafkaProducer(kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public WeatherKafkaProducer weatherKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new WeatherKafkaProducer(kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public NewsKafkaProducer newsKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new NewsKafkaProducer(kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public SanctionKafkaProducer sanctionKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new SanctionKafkaProducer(kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public GeopoliticalKafkaProducer geopoliticalKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new GeopoliticalKafkaProducer(kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public MarketKafkaProducer marketKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new MarketKafkaProducer(kafkaTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public PortCongestionKafkaProducer portCongestionKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new PortCongestionKafkaProducer(kafkaTemplate);
    }

    // --- AIS ---

    @Bean
    public AisMessageParser aisMessageParser(ObjectMapper objectMapper) {
        return new AisMessageParser(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public AisStreamWebSocketClient aisStreamWebSocketClient(
            AisMessageParser aisMessageParser,
            AisKafkaProducer aisKafkaProducer,
            ObjectMapper objectMapper,
            @Value("${aisstream.api-key}") String apiKey,
            @Value("${aisstream.websocket-url}") String websocketUrl) {
        return new AisStreamWebSocketClient(aisMessageParser, aisKafkaProducer, objectMapper, apiKey, websocketUrl);
    }

    // --- Collectors (Kafka가 활성화된 환경에서만 Bean 생성) ---

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public OpenMeteoCollector openMeteoCollector(
            WebClient.Builder webClientBuilder,
            WeatherKafkaProducer weatherKafkaProducer) {
        return new OpenMeteoCollector(webClientBuilder, weatherKafkaProducer);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public GNewsCollector gNewsCollector(
            WebClient.Builder webClientBuilder,
            NewsKafkaProducer newsKafkaProducer,
            @Value("${external-api.gnews-key}") String apiKey) {
        return new GNewsCollector(webClientBuilder, newsKafkaProducer, apiKey);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public OpenSanctionsCollector openSanctionsCollector(
            WebClient.Builder webClientBuilder,
            SanctionKafkaProducer sanctionKafkaProducer) {
        return new OpenSanctionsCollector(webClientBuilder, sanctionKafkaProducer);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public EiaOilPriceCollector eiaOilPriceCollector(
            WebClient.Builder webClientBuilder,
            MarketKafkaProducer marketKafkaProducer,
            @Value("${external-api.eia-key}") String apiKey) {
        return new EiaOilPriceCollector(webClientBuilder, marketKafkaProducer, apiKey);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public FrankfurterCollector frankfurterCollector(
            WebClient.Builder webClientBuilder,
            MarketKafkaProducer marketKafkaProducer) {
        return new FrankfurterCollector(webClientBuilder, marketKafkaProducer);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public GdeltCollector gdeltCollector(
            WebClient.Builder webClientBuilder,
            GeopoliticalKafkaProducer geopoliticalKafkaProducer) {
        return new GdeltCollector(webClientBuilder, geopoliticalKafkaProducer);
    }

    @Bean
    public UncladStatisticsDownloader uncladStatisticsDownloader(
            WebClient.Builder webClientBuilder,
            StringRedisTemplate redisTemplate) {
        return new UncladStatisticsDownloader(webClientBuilder, redisTemplate);
    }

    @Bean
    public PortCongestionCalculator portCongestionCalculator(
            StringRedisTemplate redisTemplate,
            PortRepository portRepository) {
        return new PortCongestionCalculatorImpl(redisTemplate, portRepository);
    }

    @Bean
    public UnLocodeLoader unLocodeLoader(
            PortRepository portRepository,
            WebClient.Builder webClientBuilder) {
        return new UnLocodeLoader(portRepository, webClientBuilder);
    }

    // --- Geography Loaders ---

    @Bean
    public MarineRegionsLoader marineRegionsLoader(EezRepository eezRepository) {
        return new MarineRegionsLoader(eezRepository);
    }

    @Bean
    public NaturalEarthLoader naturalEarthLoader(ChokepointRepository chokepointRepository) {
        return new NaturalEarthLoader(chokepointRepository);
    }

    @Bean
    public DemoDataInitializer demoDataInitializer(CountryRepository countryRepository,
                                                    CompanyRepository companyRepository,
                                                    VesselRepository vesselRepository,
                                                    PortRepository portRepository,
                                                    StringRedisTemplate redisTemplate) {
        return new DemoDataInitializer(countryRepository, companyRepository,
                vesselRepository, portRepository, redisTemplate);
    }

    // --- Collector Registry ---

    @Bean
    public CollectorAutoStartProperties collectorAutoStartProperties(
            @Value("${collector.auto-start.ais:false}") boolean ais,
            @Value("${collector.auto-start.weather:true}") boolean weather,
            @Value("${collector.auto-start.news:false}") boolean news,
            @Value("${collector.auto-start.oil-price:false}") boolean oilPrice,
            @Value("${collector.auto-start.exchange-rate:true}") boolean exchangeRate,
            @Value("${collector.auto-start.sanction:true}") boolean sanction,
            @Value("${collector.auto-start.geopolitical:true}") boolean geopolitical,
            @Value("${collector.auto-start.port-congestion:true}") boolean portCongestion,
            @Value("${collector.auto-start.unctad-baseline:true}") boolean unctadBaseline) {
        return new CollectorAutoStartProperties(
                ais, weather, news, oilPrice, exchangeRate,
                sanction, geopolitical, portCongestion, unctadBaseline);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public CollectorRegistry collectorRegistry(
            AisStreamClient aisStreamClient,
            CollectorAutoStartProperties autoStartProperties,
            @Value("${collector.schedule.weather:0 0 * * * *}") String weatherSchedule,
            @Value("${collector.schedule.news:0 */30 * * * *}") String newsSchedule,
            @Value("${collector.schedule.oil-price:0 0 6 * * *}") String oilPriceSchedule,
            @Value("${collector.schedule.exchange-rate:0 0 6 * * *}") String exchangeRateSchedule,
            @Value("${collector.schedule.sanction:0 0 2 * * MON}") String sanctionSchedule,
            @Value("${collector.schedule.geopolitical:0 0 */6 * * *}") String geopoliticalSchedule,
            @Value("${collector.schedule.port-congestion:0 */10 * * * *}") String portCongestionSchedule,
            @Value("${collector.schedule.unctad-baseline:0 0 4 1 1,4,7,10 *}") String unctadSchedule) {

        Map<CollectorName, String> scheduleMap = Map.of(
                CollectorName.AIS, "WebSocket (always-on)",
                CollectorName.WEATHER, weatherSchedule,
                CollectorName.NEWS, newsSchedule,
                CollectorName.OIL_PRICE, oilPriceSchedule,
                CollectorName.EXCHANGE_RATE, exchangeRateSchedule,
                CollectorName.SANCTION, sanctionSchedule,
                CollectorName.GEOPOLITICAL, geopoliticalSchedule,
                CollectorName.PORT_CONGESTION, portCongestionSchedule,
                CollectorName.UNCTAD_BASELINE, unctadSchedule
        );

        return new CollectorRegistryImpl(aisStreamClient, autoStartProperties, scheduleMap);
    }
}
