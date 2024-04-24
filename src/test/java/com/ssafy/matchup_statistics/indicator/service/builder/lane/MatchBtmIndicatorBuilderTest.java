package com.ssafy.matchup_statistics.indicator.service.builder.lane;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.ssafy.matchup_statistics.global.api.rest.RiotRestApiAdaptor;
import com.ssafy.matchup_statistics.global.config.TestConfiguration;
import com.ssafy.matchup_statistics.indicator.entity.Indicator;
import com.ssafy.matchup_statistics.indicator.entity.match.LaneInfo;
import com.ssafy.matchup_statistics.indicator.entity.match.MatchIndicator;
import com.ssafy.matchup_statistics.indicator.entity.match.TeamPosition;
import com.ssafy.matchup_statistics.indicator.service.builder.IndicatorBuilder;
import com.ssafy.matchup_statistics.global.dto.response.MatchDetailResponseDto;
import com.ssafy.matchup_statistics.global.dto.response.MatchTimelineResponseDto;
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
class MatchBtmIndicatorBuilderTest {
    @Mock
    RiotRestApiAdaptor riotRestApiAdaptor;

    @Mock
    MongoTemplate mongoTemplate;

    @InjectMocks
    IndicatorBuilder target = new IndicatorBuilder(riotRestApiAdaptor, mongoTemplate);

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

        // 본인 아이디 : 9
        // 상대 아이디 : 4
        // 라인 : 원딜
        List<String> matches = new ArrayList<>();
        matches.add("KR_6994313306");

        puuid = "fjIln83OKmjB4M5IHSnRL3_3ugrRH-PeMfHfQX0A7eTEsfVss4ae-5q97bfmlqOhuNS9ty_McT65Cg";
        summonerId = "XpfTc8FZVplKFNyQJyIXDbHwspU2I0qL2yjau8S7y5qk2w";
        given(riotRestApiAdaptor.getMatchIdsByPuuid(puuid)).willReturn(matches);
        given(riotRestApiAdaptor.getMatchDetailResponseDtoByMatchId("KR_6994313306")).willReturn(matchDetailResponseDto);
        given(riotRestApiAdaptor.getMatchTimelineResponseDtoByMatchId("KR_6994313306")).willReturn(matchTimelineResponseDto);

        // 라인 정보 빌드
        laneInfo = LaneInfo.builder()
                .teamPosition(TeamPosition.BOTTOM)
                .isBottomLane(true)
                .myLaneNumber(9)
                .myTeamId(200)
                .oppositeLaneNumber(4)
                .myBottomDuoNumber(10)
                .oppositeBottomDuoNumber(5)
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
    @DisplayName("원딜 기초체력 : 경험치, cs 차이 확인")
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
                .isEqualTo(5100 - 5199);
        assertThat(indicator.getMatchIndicators().get(0)
                .getLaneIndicator()
                .getBasicWeight()
                .getCsDiffer())
                .isEqualTo(121 - 143);
    }

    @Test
    @Order(3)
    @DisplayName("원딜 기초체력 : 포골차이 확인")
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
                .getTurretPlateDestroyDiffer())
                .isEqualTo(2 - 2);
    }

    @Test
    @Order(4)
    @DisplayName("원딜 공격적인 라인전 : 솔킬 및 듀오킬 차이 확인")
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
    @DisplayName("원딜 공격적인 라인전 : 딜량차이 확인")
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
                .isEqualTo(8081 - 7108);
    }
}