package com.anchoriq.api.controller.debug;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 디버그 엔드포인트 (임시).
 */
@RestController
@RequestMapping("/api/debug/redis")
@RequiredArgsConstructor
public class RedisDebugController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Simple SET/GET
        try {
            redisTemplate.opsForValue().set("debug:test", "hello");
            String val = redisTemplate.opsForValue().get("debug:test");
            redisTemplate.delete("debug:test");
            result.put("setGet", val);
        } catch (Exception e) {
            result.put("setGetError", e.getMessage());
        }

        // 2. GEO ADD
        try {
            Long added = redisTemplate.opsForGeo().add("debug:geo",
                    new Point(126.9, 37.5), "test-vessel");
            result.put("geoAdd", added);

            Long count = redisTemplate.opsForZSet().zCard("debug:geo");
            result.put("geoCount", count);

            List<Point> pos = redisTemplate.opsForGeo().position("debug:geo", "test-vessel");
            result.put("geoPosition", pos);

            redisTemplate.delete("debug:geo");
        } catch (Exception e) {
            result.put("geoError", e.getMessage());
        }

        // 3. Check vessels:positions
        try {
            Long vesselCount = redisTemplate.opsForZSet().zCard("vessels:positions");
            result.put("vesselGeoCount", vesselCount);

            Set<String> members = redisTemplate.opsForZSet().range("vessels:positions", 0, 4);
            result.put("vesselSample", members);
        } catch (Exception e) {
            result.put("vesselError", e.getMessage());
        }

        // 4. Connection info
        try {
            String ping = redisTemplate.getConnectionFactory().getConnection().ping();
            result.put("ping", ping);
        } catch (Exception e) {
            result.put("pingError", e.getMessage());
        }

        return result;
    }
}
