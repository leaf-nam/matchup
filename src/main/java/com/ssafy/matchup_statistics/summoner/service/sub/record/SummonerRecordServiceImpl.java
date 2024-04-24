package com.ssafy.matchup_statistics.summoner.service.sub.record;

import com.ssafy.matchup_statistics.global.dao.UserDao;
import com.ssafy.matchup_statistics.global.dto.jpa.UserRiotAccountDto;
import com.ssafy.matchup_statistics.global.exception.RiotApiException;
import com.ssafy.matchup_statistics.global.exception.RiotDataError;
import com.ssafy.matchup_statistics.global.exception.RiotDataException;
import com.ssafy.matchup_statistics.indicator.entity.Indicator;
import com.ssafy.matchup_statistics.match.entity.Match;
import com.ssafy.matchup_statistics.summoner.dto.response.SummonerRecordInfoResponseDto;
import com.ssafy.matchup_statistics.summoner.entity.Summoner;
import com.ssafy.matchup_statistics.summoner.service.builder.SummonerBuilder;
import com.ssafy.matchup_statistics.summoner.dao.SummonerDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("summonerRecordServiceImpl")
@RequiredArgsConstructor
@Slf4j
public class SummonerRecordServiceImpl implements SummonerRecordService {

    private final SummonerDao summonerDao;
    private final UserDao userDao;
    private final SummonerBuilder summonerBuilder;

    @Override
    public SummonerRecordInfoResponseDto getSummonerRecordInfo(String gameName, String tagLine) {

        try{
            Summoner summonerInDB = summonerDao.getSummonerInDB(gameName, tagLine);
            Indicator indicatorInDB = summonerDao.getIndicatorInDB(summonerInDB.getId());
            List<Match> matches = summonerInDB.getMatchIds().stream().map(summonerDao::getMatchInDB).toList();
            SummonerRecordInfoResponseDto summonerRecordInfoResponseDto = new SummonerRecordInfoResponseDto(summonerInDB, indicatorInDB, matches);
            log.info("SummonerRecordInfo create : {}", summonerRecordInfoResponseDto);
            return summonerRecordInfoResponseDto;

            // DB에 없을 경우 라이엇에서 찾아오기
        } catch (RiotDataException e) {
            log.debug("error : {}", e.getRiotDataError());
            if (e.getRiotDataError().equals(RiotDataError.NOT_IN_STATISTICS_DATABASE)){
                return summonerBuilder.buildRecord(gameName, tagLine);
            } else throw new RiotDataException(e.getRiotDataError());
        }
    }

    @Override
    public SummonerRecordInfoResponseDto getSummonerRecordInfo(long userId) {
        UserRiotAccountDto userRiotAccountDto = userDao.getUserRiotAccountDto(userId);

        return getSummonerRecordInfo(userRiotAccountDto.getRiotAccountInfo().getSummonerProfile().getName(),
                userRiotAccountDto.getRiotAccountInfo().getSummonerProfile().getTag());
    }

    @Override
    public void saveSummonerRecordInfo(Long userId) {
        UserRiotAccountDto userRiotAccountDto = userDao.getUserRiotAccountDto(userId);

        summonerBuilder.buildRecord(userRiotAccountDto.getRiotAccountInfo().getSummonerProfile().getName(),
                userRiotAccountDto.getRiotAccountInfo().getSummonerProfile().getTag());
    }

    @Override
    public void saveSummonerRecordInfo(String gameName, String tagLine) {
        summonerBuilder.buildRecord(gameName, tagLine);
    }
}
