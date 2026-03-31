package com.anchoriq.collector.source.ais;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

/**
 * AIS 메시지 파서.
 * AISstream.io WebSocket에서 수신한 JSON을 Kafka 전송용 메시지로 변환한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class AisMessageParser {

    private static final Logger log = LoggerFactory.getLogger(AisMessageParser.class);
    private static final Map<Integer, String> MID_TO_COUNTRY = initMidTable();
    private final ObjectMapper objectMapper;

    public AisMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * AISstream 원본 메시지를 파싱하여 Kafka 전송용 맵으로 변환한다.
     */
    public Map<String, Object> parse(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            JsonNode metaData = root.path("MetaData");
            JsonNode message = root.path("Message").path("PositionReport");

            if (message.isMissingNode()) {
                return null;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mmsi", String.valueOf(metaData.path("MMSI").asInt()));
            result.put("imo", metaData.path("IMO").asText(""));
            result.put("name", metaData.path("ShipName").asText("UNKNOWN").trim());
            result.put("type", resolveVesselType(metaData.path("ShipType").asInt()));
            String flag = metaData.path("country_code").asText("");
            if (flag.isEmpty()) {
                flag = resolveCountryFromMmsi(String.valueOf(metaData.path("MMSI").asInt()));
            }
            result.put("flag", flag);
            result.put("lat", message.path("Latitude").asDouble());
            result.put("lon", message.path("Longitude").asDouble());
            result.put("speed", message.path("Sog").asDouble());
            result.put("heading", message.path("TrueHeading").asInt());
            result.put("status", resolveNavigationStatus(message.path("NavigationalStatus").asInt()));
            result.put("timestamp", metaData.path("time_utc").asText());

            return result;
        } catch (Exception e) {
            log.warn("Failed to parse AIS message: {}", e.getMessage());
            return null;
        }
    }

    public String extractMmsi(Map<String, Object> parsedMessage) {
        Object mmsi = parsedMessage.get("mmsi");
        return mmsi != null ? mmsi.toString() : null;
    }

    private String resolveVesselType(int shipType) {
        if (shipType >= 80 && shipType <= 89) return "TANKER";
        if (shipType >= 70 && shipType <= 79) return "CONTAINER";
        if (shipType >= 60 && shipType <= 69) return "PASSENGER";
        if (shipType >= 40 && shipType <= 49) return "FISHING";
        if (shipType >= 30 && shipType <= 39) return "TUG";
        if (shipType >= 20 && shipType <= 29) return "BULK_CARRIER";
        if (shipType == 90) return "OTHER";
        if (shipType == 91 || shipType == 92) return "GENERAL_CARGO";
        return "OTHER";
    }

    private String resolveCountryFromMmsi(String mmsi) {
        if (mmsi == null || mmsi.length() < 3) return "";
        try {
            int mid = Integer.parseInt(mmsi.substring(0, 3));
            return MID_TO_COUNTRY.getOrDefault(mid, "");
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static Map<Integer, String> initMidTable() {
        Map<Integer, String> m = new HashMap<>();
        m.put(201, "AL"); m.put(202, "AD"); m.put(203, "AT"); m.put(204, "PT");
        m.put(205, "BE"); m.put(206, "BY"); m.put(207, "BG"); m.put(208, "VA");
        m.put(209, "CY"); m.put(210, "CY"); m.put(211, "DE"); m.put(212, "CY");
        m.put(213, "GE"); m.put(214, "MD"); m.put(215, "MT"); m.put(216, "AM");
        m.put(218, "DE"); m.put(219, "DK"); m.put(220, "DK"); m.put(224, "ES");
        m.put(225, "ES"); m.put(226, "FR"); m.put(227, "FR"); m.put(228, "FR");
        m.put(229, "MT"); m.put(230, "FI"); m.put(231, "FO"); m.put(232, "GB");
        m.put(233, "GB"); m.put(234, "GB"); m.put(235, "GB"); m.put(236, "GI");
        m.put(237, "GR"); m.put(238, "HR"); m.put(239, "GR"); m.put(240, "GR");
        m.put(241, "GR"); m.put(242, "MA"); m.put(243, "HU"); m.put(244, "NL");
        m.put(245, "NL"); m.put(246, "NL"); m.put(247, "IT"); m.put(248, "MT");
        m.put(249, "MT"); m.put(250, "IE"); m.put(251, "IS"); m.put(252, "LI");
        m.put(253, "LU"); m.put(254, "MC"); m.put(255, "PT"); m.put(256, "MT");
        m.put(257, "NO"); m.put(258, "NO"); m.put(259, "NO"); m.put(261, "PL");
        m.put(263, "PT"); m.put(264, "RO"); m.put(265, "SE"); m.put(266, "SE");
        m.put(267, "SK"); m.put(268, "SM"); m.put(269, "CH"); m.put(270, "CZ");
        m.put(271, "TR"); m.put(272, "UA"); m.put(273, "RU"); m.put(274, "MK");
        m.put(275, "LV"); m.put(276, "EE"); m.put(277, "LT"); m.put(278, "SI");
        m.put(279, "RS"); m.put(301, "AI"); m.put(303, "US"); m.put(304, "AG");
        m.put(305, "AG"); m.put(306, "CW"); m.put(307, "AW"); m.put(308, "BS");
        m.put(309, "BS"); m.put(310, "BM"); m.put(311, "BS"); m.put(312, "BZ");
        m.put(314, "BB"); m.put(316, "CA"); m.put(319, "KY"); m.put(321, "CR");
        m.put(323, "CU"); m.put(325, "DM"); m.put(327, "DO"); m.put(329, "GP");
        m.put(330, "GD"); m.put(331, "GL"); m.put(332, "GT"); m.put(334, "HN");
        m.put(336, "HT"); m.put(338, "US"); m.put(339, "JM"); m.put(341, "KN");
        m.put(343, "LC"); m.put(345, "MX"); m.put(347, "MQ"); m.put(348, "MS");
        m.put(350, "NI"); m.put(351, "PA"); m.put(352, "PA"); m.put(353, "PA");
        m.put(354, "PA"); m.put(355, "PA"); m.put(356, "PA"); m.put(357, "PA");
        m.put(358, "PR"); m.put(359, "SV"); m.put(361, "PM"); m.put(362, "TT");
        m.put(364, "TC"); m.put(366, "US"); m.put(367, "US"); m.put(368, "US");
        m.put(369, "US"); m.put(370, "PA"); m.put(371, "PA"); m.put(372, "PA");
        m.put(373, "PA"); m.put(374, "PA"); m.put(375, "VC"); m.put(376, "VC");
        m.put(377, "VC"); m.put(378, "VG"); m.put(379, "VI");
        m.put(401, "AF"); m.put(403, "SA"); m.put(405, "BD"); m.put(408, "BH");
        m.put(410, "BT"); m.put(412, "CN"); m.put(413, "CN"); m.put(414, "CN");
        m.put(416, "TW"); m.put(417, "LK"); m.put(419, "IN"); m.put(422, "IR");
        m.put(423, "AZ"); m.put(425, "IQ"); m.put(428, "IL"); m.put(431, "JP");
        m.put(432, "JP"); m.put(434, "TM"); m.put(436, "KZ"); m.put(437, "UZ");
        m.put(438, "JO"); m.put(440, "KR"); m.put(441, "KR"); m.put(443, "PS");
        m.put(445, "KP"); m.put(447, "KW"); m.put(450, "LB"); m.put(451, "KG");
        m.put(453, "MO"); m.put(455, "MV"); m.put(457, "MN"); m.put(459, "NP");
        m.put(461, "OM"); m.put(463, "PK"); m.put(466, "QA"); m.put(468, "SY");
        m.put(470, "AE"); m.put(472, "TJ"); m.put(473, "YE"); m.put(475, "AF");
        m.put(477, "HK"); m.put(478, "BA");
        m.put(501, "FR"); m.put(503, "AU"); m.put(506, "MM"); m.put(508, "BN");
        m.put(510, "FM"); m.put(511, "PW"); m.put(512, "NZ"); m.put(514, "KH");
        m.put(515, "KH"); m.put(516, "CX"); m.put(518, "CK"); m.put(520, "FJ");
        m.put(523, "CC"); m.put(525, "ID"); m.put(529, "KI"); m.put(531, "LA");
        m.put(533, "MY"); m.put(536, "MP"); m.put(538, "MH"); m.put(540, "NC");
        m.put(542, "NU"); m.put(544, "NR"); m.put(546, "PF"); m.put(548, "PH");
        m.put(553, "PG"); m.put(555, "PN"); m.put(557, "SB"); m.put(559, "AS");
        m.put(561, "WS"); m.put(563, "SG"); m.put(564, "SG"); m.put(565, "SG");
        m.put(566, "SG"); m.put(567, "TH"); m.put(570, "TO"); m.put(572, "TV");
        m.put(574, "VN"); m.put(576, "VU"); m.put(577, "VU"); m.put(578, "WF");
        m.put(601, "ZA"); m.put(603, "AO"); m.put(605, "DZ"); m.put(607, "FR");
        m.put(608, "IO"); m.put(609, "BI"); m.put(610, "BJ"); m.put(611, "BW");
        m.put(612, "CM"); m.put(613, "CV"); m.put(615, "CG"); m.put(616, "KM");
        m.put(617, "CI"); m.put(618, "CD"); m.put(619, "DJ"); m.put(620, "EG");
        m.put(621, "GQ"); m.put(622, "ET"); m.put(624, "ER"); m.put(625, "GA");
        m.put(626, "GH"); m.put(627, "GM"); m.put(629, "GN"); m.put(630, "GW");
        m.put(631, "GN"); m.put(632, "LR"); m.put(633, "LR"); m.put(634, "LR");
        m.put(635, "LR"); m.put(636, "LR"); m.put(637, "LR"); m.put(638, "SS");
        m.put(642, "LY"); m.put(644, "LS"); m.put(645, "MU"); m.put(647, "MG");
        m.put(649, "ML"); m.put(650, "MZ"); m.put(654, "MR"); m.put(655, "MW");
        m.put(656, "NE"); m.put(657, "NG"); m.put(659, "NA"); m.put(660, "RE");
        m.put(661, "RW"); m.put(662, "ST"); m.put(663, "SN"); m.put(664, "SC");
        m.put(665, "SH"); m.put(666, "SO"); m.put(667, "SL"); m.put(668, "SZ");
        m.put(669, "SD"); m.put(670, "TD"); m.put(671, "TG"); m.put(672, "TN");
        m.put(674, "TZ"); m.put(675, "UG"); m.put(676, "TZ"); m.put(677, "TZ");
        m.put(678, "ZM"); m.put(679, "ZW");
        m.put(701, "AR"); m.put(710, "BR"); m.put(720, "BO"); m.put(725, "CL");
        m.put(730, "CO"); m.put(735, "EC"); m.put(740, "FK"); m.put(745, "GF");
        m.put(750, "GY"); m.put(755, "PY"); m.put(760, "PE"); m.put(765, "SR");
        m.put(770, "UY"); m.put(775, "VE");
        return m;
    }

    private String resolveNavigationStatus(int status) {
        return switch (status) {
            case 0 -> "SAILING";
            case 1 -> "ANCHORED";
            case 5 -> "MOORED";
            case 2 -> "NOT_UNDER_COMMAND";
            case 3 -> "RESTRICTED_MANEUVERABILITY";
            case 6 -> "AGROUND";
            case 7 -> "FISHING_ENGAGED";
            default -> "UNKNOWN";
        };
    }
}
