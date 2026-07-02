package com.pwp.core.model;

import java.util.List;

public class MatchResult {
    public String mapName;
    public String mode;
    public int teamBlueScore;
    public int teamRedScore;
    public String winner;
    public int durationSeconds;
    public String startedAt;
    public String endedAt;
    public List<MatchPlayer> players;
}
