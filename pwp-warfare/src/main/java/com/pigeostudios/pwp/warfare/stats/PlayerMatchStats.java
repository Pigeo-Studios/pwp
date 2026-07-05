package com.pigeostudios.pwp.warfare.stats;

public class PlayerMatchStats {

    public String uuid;
    public String nickname;
    public String team;

    public int kills;
    public int deaths;
    public int assists;
    public int score;

    public int vehicleKills;
    public int captures;
    public int revives;
    public int shotsFired;
    public int shotsHit;
    public double damageDealt;
    public double healingDone;
    public int suppliesDelivered;
    public double longestKill;

    public String role;
    public int squadId;
    public boolean wasSquadLeader;

    public int killStreak;
    public int bestKillStreak;

    public long lastKillTime;

    public int vehiclesDestroyed;
    public int airVehiclesDestroyed;
    public int teamKills;
    public int headshots;
    public int hubDestructions;
    public int baseDefends;

    public double distanceTraveled;

    public PlayerMatchStats(String uuid, String nickname, String team) {
        this.uuid = uuid;
        this.nickname = nickname;
        this.team = team;
    }

    public void recordKill() {
        kills++;
        killStreak++;
        if (killStreak > bestKillStreak) bestKillStreak = killStreak;
        score += 100;
    }

    public void recordDeath() {
        deaths++;
        killStreak = 0;
    }

    public void recordAssist() {
        assists++;
        score += 25;
    }

    public void recordCapture() {
        captures++;
        score += 200;
    }

    public void recordRevive() {
        revives++;
        score += 75;
    }

    public void recordVehicleKill() {
        vehicleKills++;
        score += 150;
    }

    public void recordDamage(double dmg) {
        damageDealt += dmg;
    }

    public void recordHealing(double heal) {
        healingDone += heal;
    }

    public void recordShot(boolean hit) {
        shotsFired++;
        if (hit) shotsHit++;
    }

    public void recordVehicleDestroyed(boolean isAir) {
        vehiclesDestroyed++;
        if (isAir) airVehiclesDestroyed++;
        score += 100;
    }

    public void recordTeamKillStat() {
        teamKills++;
    }

    public void recordHeadshot() {
        headshots++;
        score += 25;
    }

    public void recordHubDestruction() {
        hubDestructions++;
        score += 300;
    }

    public void recordBaseDefend() {
        baseDefends++;
        score += 50;
    }

    public void recordDistance(double dist) {
        distanceTraveled += dist;
    }

    public double getAccuracy() {
        return shotsFired == 0 ? 0 : (double) shotsHit / shotsFired;
    }

    public double getKD() {
        return deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
    }
}
