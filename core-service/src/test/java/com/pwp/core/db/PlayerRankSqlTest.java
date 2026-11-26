package com.pwp.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Ранг игрока: подзапрос со своими псевдонимами (иначе MySQL коррелирует с внешним ps — у всех место 1). */
class PlayerRankSqlTest {
    private static int count(String s, String sub) {
        int n = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) n++;
        return n;
    }

    @Test
    void subqueryDeclaresOwnAliases() {
        for (String by : new String[] {"kills", "kd", "winrate", "score", "level"}) {
            String sql = PlayerRepository.rankSql(PlayerRepository.rankColumn(by));
            String sub = sql.substring(sql.indexOf("(SELECT"));
            assertTrue(sub.contains("FROM player_stats ps JOIN player_xp px ON ps.uuid = px.uuid"), by);
            assertTrue(sub.contains("WHERE ps.uuid = ?"), by);
            assertEquals(2, count(sql, "FROM player_stats ps"), by);
        }
    }

    @Test
    void unknownOrderFallsBackToKills() {
        assertEquals("ps.kills", PlayerRepository.rankColumn("drop table"));
    }
}
