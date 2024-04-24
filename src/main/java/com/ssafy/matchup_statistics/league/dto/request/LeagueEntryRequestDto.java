package com.ssafy.matchup_statistics.league.dto.request;

import com.ssafy.matchup_statistics.league.type.Division;
import com.ssafy.matchup_statistics.league.type.Queue;
import com.ssafy.matchup_statistics.league.type.Tier;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeagueEntryRequestDto {
    @NotBlank
    private Division division;
    @NotBlank
    private Tier tier;
    @NotBlank
    private Queue queue;

    public String getLeagueEntryRequestUrl() {
        return "/" + queue + "/" + tier + "/" + division;
    }
}
