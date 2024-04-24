package com.ssafy.matchup_statistics.indicator.service.builder.lane;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.ssafy.matchup_statistics.global.api.rest.RiotRestApiAdaptor;
import com.ssafy.matchup_statistics.global.config.TestConfiguration;
import com.ssafy.matchup_statistics.global.dto.response.MatchDetailResponseDto;
import com.ssafy.matchup_statistics.global.dto.response.MatchTimelineResponseDto;
import com.ssafy.matchup_statistics.indicator.entity.Indicator;
import com.ssafy.matchup_statistics.indicator.entity.match.LaneInfo;
import com.ssafy.matchup_statistics.indicator.entity.match.MatchIndicator;
import com.ssafy.matchup_statistics.indicator.entity.match.TeamPosition;
import com.ssafy.matchup_statistics.indicator.service.builder.IndicatorBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ContextConfiguration(classes = TestConfiguration.class)
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
@Tag("MatchLaneIndicatorBuilderTest")
@Tag("IndicatorTotalTest")
class MatchUtilIndicatorBuilderTest {

    @Mock
    RiotRestApiAdaptor riotRestApiAdaptor;

    @Mock
    MongoTemplate mongoTemplate;
    @InjectMocks
    IndicatorBuilder target;

    @Qualifier("kang_chan_bob_detail")
    @Autowired
    MatchDetailResponseDto matchDetailResponseDto;

    @Qualifier("kang_chan_bob_timeline")
    @Autowired
    MatchTimelineResponseDto matchTimelineResponseDto;


    LaneInfo laneInfo;
    MatchIndicator.Metadata metadata;
    String puuid;
    String summonerId;


    @BeforeAll
    public static void initLog() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.ERROR);
    }

    @BeforeEach
    void init() {
        target = new IndicatorBuilder(riotRestApiAdaptor, mongoTemplate);

        // 본인 아이디 : 10
        // 상대 아이디 : 5
        // 라인 : 서폿
        List<String> matches = new ArrayList<>();
        matches.add("KR_6994313306");

        puuid = "YFcHIr9qo-qHWo2Wja0LPvCKrxNOHG3TFEi2szdBD8AJB3V4iSy2IZf8oReZpxl27aPElWlNwVXgjQ";
        summonerId = "XpfTc8FZVplKFNyQJyIXDbHwspU2I0qL2yjau8S7y5qk2w";
        given(riotRestApiAdaptor.getMatchIdsByPuuid(puuid)).willReturn(matches);
        given(riotRestApiAdaptor.getMatchDetailResponseDtoByMatchId("KR_6994313306")).willReturn(matchDetailResponseDto);
        given(riotRestApiAdaptor.getMatchTimelineResponseDtoByMatchId("KR_6994313306")).willReturn(matchTimelineResponseDto);

        // 라인 정보 빌드
        laneInfo = LaneInfo.builder()
                .teamPosition(TeamPosition.UTILITY)
                .isBottomLane(true)
                .myLaneNumber(10)
                .myTeamId(200)
                .oppositeLaneNumber(5)
                .myBottomDuoNumber(9)
                .oppositeBottomDuoNumber(4)
                .build();

        // 메타정보 빌드
        metadata = MatchIndicator.Metadata.builder()
                .laneInfo(laneInfo)
                .isFinishedBeforeFifteen(false)
                .build();

    }

    @Test
    @Order(1)
    @DisplayName("라인 정보 잘 가져오는지 테스트")
    void laneInfoTest() {
        // given

        // when
        Indicator indicator = target.build(
                riotRestApiAdaptor.getMatchIdsByPuuid(puuid),
                summonerId, puuid
        );

        // then
        assertThat(indicator.getMatchIndicators().get(0)
                .getMetadata()
                .getLaneInfo())
                .usingRecursiveComparison()
                .isEqualTo(laneInfo);
    }

    @Test
    @Order(2)
    @DisplayName("서폿 기초체력 : 경험치 차이 확인")
    void xpCsDifferTest() {
        // given

        // when
        Indicator indicator = target.build(
                riotRestApiAdaptor.getMatchIdsByPuuid(puuid),
                summonerId, puuid
        );

        // then
        assertThat(indicator.getMatchIndicators().get(0)
                .getLaneIndicator()
                .getBasicWeight()
                .getExpDiffer())
                .isEqualTo(3885 - 4224);
    }

    @Test
    @Order(3)
    @DisplayName("서폿 기초체력 : 서폿템 완성시간 확인")
    void towerGoldDifferTest() {
        // given

        // when
        Indicator indicator = target.build(
                riotRestApiAdaptor.getMatchIdsByPuuid(puuid),
                summonerId, puuid
        );

        // then
        assertThat(indicator.getMatchIndicators().get(0)
                .getLaneIndicator()
                .getBasicWeight()
                .getSupportItemFinishedTimeDiffer())
                .isEqualTo(806527 - 897281);
    }

    @Test
    @Order(4)
    @DisplayName("서폿 공격적인 라인전 : 솔킬 및 듀오킬 차이 확인")
    void solokillDifferTest() {
        // given

        // when
        Indicator indicator = target.build(
                riotRestApiAdaptor.getMatchIdsByPuuid(puuid),
                summonerId, puuid
        );

        // then
        assertThat(indicator.getMatchIndicators().get(0)
                .getLaneIndicator()
                .getAggresiveLaneAbility()
                .getDuoKillDiffer())
                .isEqualTo(2 - 0);
    }


    @Test
    @Order(5)
    @DisplayName("서폿 공격적인 라인전 : 딜량차이 확인")
    void dealDifferTest() {
        // given

        // when
        Indicator indicator = target.build(
                riotRestApiAdaptor.getMatchIdsByPuuid(puuid),
                summonerId, puuid
        );

        // then
        assertThat(indicator.getMatchIndicators().get(0)
                .getLaneIndicator()
                .getAggresiveLaneAbility()
                .getDealDiffer())
                .isEqualTo(1966 - 2468);
    }
}