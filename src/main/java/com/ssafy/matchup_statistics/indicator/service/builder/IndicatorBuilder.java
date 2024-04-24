package com.ssafy.matchup_statistics.indicator.service.builder;

import com.ssafy.matchup_statistics.global.api.rest.RiotRestApiAdaptor;
import com.ssafy.matchup_statistics.global.dto.response.MatchDetailResponseDto;
import com.ssafy.matchup_statistics.global.dto.response.MatchTimelineResponseDto;
import com.ssafy.matchup_statistics.global.exception.RiotDataError;
import com.ssafy.matchup_statistics.global.exception.RiotDataException;
import com.ssafy.matchup_statistics.indicator.entity.Indicator;
import com.ssafy.matchup_statistics.indicator.entity.match.MatchIndicator;
import com.ssafy.matchup_statistics.indicator.entity.match.TeamPosition;
import com.ssafy.matchup_statistics.indicator.entity.match.TimeInfo;
import com.ssafy.matchup_statistics.match.entity.Match;
import com.ssafy.matchup_statistics.summoner.dto.response.RecordMatchDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class IndicatorBuilder {

    private final RiotRestApiAdaptor riotRestApiAdaptor;
    private final MongoTemplate mongoTemplate;

    public Indicator build(List<String> matchIds, String summonerId, String puuid) {
        List<MatchIndicator> matchIndicators = new ArrayList<>();

        // 검색한 매치들에 대해 각각 세부정보 조회
        log.debug("분석할 매치 배열 : {}", matchIds);
        matchIds.forEach(matchId -> {
            long start = System.currentTimeMillis();
            log.debug("매치 id({}) 분석 시작", matchId);
            MatchDetailResponseDto matchDetailResponseDtoByMatchId = riotRestApiAdaptor.getMatchDetailResponseDtoByMatchId(matchId);
            MatchTimelineResponseDto matchTimelineResponseDtoByMatchId = riotRestApiAdaptor.getMatchTimelineResponseDtoByMatchId(matchId);

            // 매치정보는 별도로 저장
            mongoTemplate.save(new Match(matchId, new RecordMatchDetail(matchDetailResponseDtoByMatchId)));

            // 15분 이전에 끝난 게임 처리
            if (matchTimelineResponseDtoByMatchId.getInfo().getFrames().size() <= 15) {
                TimeInfo timeInfo = new TimeInfo(matchDetailResponseDtoByMatchId.getInfo().gameDuration,
                        matchDetailResponseDtoByMatchId.getInfo().getGameStartTimestamp(),
                        matchDetailResponseDtoByMatchId.getInfo().getGameEndTimestamp());
                matchIndicators.add(new MatchIndicator(matchId, timeInfo, true));
                log.debug("15분 이전에 종료한 게임 : {}", matchId);
                return;
            }

            // teamPosition이 없는 경우 처리
            try {
                matchDetailResponseDtoByMatchId.getInfo().getParticipants().forEach(p -> {
                    if (!EnumUtils.isValidEnumIgnoreCase(TeamPosition.class, p.getTeamPosition()))
                        throw new RiotDataException(RiotDataError.ILLEGAL_TEAM_POSITION_ERROR);
                });
            } catch (RiotDataException riotDataException) {
                TimeInfo timeInfo = new TimeInfo(matchDetailResponseDtoByMatchId.getInfo().gameDuration,
                        matchDetailResponseDtoByMatchId.getInfo().getGameStartTimestamp(),
                        matchDetailResponseDtoByMatchId.getInfo().getGameEndTimestamp());
                matchIndicators.add(new MatchIndicator(matchId, timeInfo, false));
                log.debug("포지션 정보를 받아올 수 없는 게임 : {}", matchId);
                return;
            }

            // 세부정보를 통해 매치 지표 생성
            MatchIndicator matchIndicator = new MatchIndicator(puuid, matchDetailResponseDtoByMatchId, matchTimelineResponseDtoByMatchId);
            log.debug("matchIndicator(매치 지표) 생성완료 : {}ms 소요", System.currentTimeMillis() - start);

            matchIndicators.add(matchIndicator);
        });

        matchIndicators.sort((i1, i2) -> (int) (i2.getMetadata().getTimeInfo().getStartTime() - i1.getMetadata().getTimeInfo().getStartTime()));
        log.debug("생성된 매치 지표들 확인 : {}", matchIndicators);

        return new Indicator(summonerId, matchIndicators);
    }


}
