package com.ssafy.matchup_statistics.indicator.entity.match.beginning;

import com.ssafy.matchup_statistics.global.dto.response.MatchTimelineResponseDto;
import com.ssafy.matchup_statistics.indicator.entity.match.TeamPosition;
import com.ssafy.matchup_statistics.indicator.entity.match.LaneInfo;
import com.ssafy.matchup_statistics.indicator.entity.match.beginning.base.AggresiveLaneAbility;
import com.ssafy.matchup_statistics.indicator.entity.match.beginning.base.BasicWeight;
import com.ssafy.matchup_statistics.indicator.data.Before_15_Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public abstract class LaneIndicator {
    private BasicWeight basicWeight = new BasicWeight();
    private AggresiveLaneAbility aggresiveLaneAbility = new AggresiveLaneAbility();

    public LaneIndicator(LaneInfo laneInfo, MatchTimelineResponseDto matchTimelineResponseDto) {
        // 15분까지 데이터 모두 확인
        Before_15_Data before15Data = new Before_15_Data(laneInfo, matchTimelineResponseDto);

        // 15분 데이터만 확인
        MatchTimelineResponseDto.ParticipantNumber my15Data = getMy_15Data(laneInfo, matchTimelineResponseDto);
        MatchTimelineResponseDto.ParticipantNumber opposite15Data = getOpposite_15Data(laneInfo, matchTimelineResponseDto);

        // 기본체급 생성
        this.getBasicWeight().setBasicWeightByDataBefore_15(before15Data);
        this.getBasicWeight().setBasicWeightByData_15(laneInfo, my15Data, opposite15Data);

        // 공격적인 라인전 생성
        this.getAggresiveLaneAbility().setAggresiveLaneAbilityByDataBefore_15(before15Data);
        this.getAggresiveLaneAbility().setAggresiveLaneAbilityByData_15(my15Data, opposite15Data);

        if (laneInfo.getTeamPosition().equals(TeamPosition.JUNGLE)) {
            ((JgIndicator) this).addJgIndicator(before15Data, my15Data, opposite15Data);
        }
    }

    protected MatchTimelineResponseDto.ParticipantNumber getMy_15Data(LaneInfo laneInfo, MatchTimelineResponseDto matchTimelineResponseDto) {
        return matchTimelineResponseDto.getInfo().getFrames().get(15).getParticipantFrames()
                .getDataByNumber(laneInfo.getMyLaneNumber());
    }

    protected MatchTimelineResponseDto.ParticipantNumber getOpposite_15Data(LaneInfo laneInfo, MatchTimelineResponseDto matchTimelineResponseDto) {
        return matchTimelineResponseDto.getInfo().getFrames().get(15).getParticipantFrames()
                .getDataByNumber(laneInfo.getOppositeLaneNumber());
    }
}
