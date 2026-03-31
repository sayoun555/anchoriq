package com.anchoriq.ai.whatif;

import java.util.List;
import java.util.Map;

/**
 * What-if 시뮬레이션 서비스 인터페이스.
 */
public interface WhatIfService {

    WhatIfResult simulate(String scenario, String duration);

    List<WhatIfResult> getHistory(Long userId, int limit);

    List<WhatIfTemplate> getTemplates();
}
