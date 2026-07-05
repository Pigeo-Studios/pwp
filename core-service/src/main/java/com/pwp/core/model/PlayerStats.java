package com.pwp.core.model;

public class PlayerStats {
    public String uuid;
    public int kills;
    public int deaths;
    public int wins;
    public int losses;
    public long playtimeSeconds;
    public int shotsFired;
    public int shotsHit;
    public int revives;
    public int vehicleKills;
    public int captures;
    public double damageDealt;
    public double healingDone;
    public int suppliesDelivered;
    public double longestKill;
    public int bestKillStreak;
    public int matchesPlayed;
    public int vehiclesDestroyed;
    public int airVehiclesDestroyed;
    public int teamKills;
    public int currentWinStreak;
    public int bestWinStreak;
    public long survivalTime;
    public int headshots;

    public double getKd() {
        return deaths == 0 ? kills : (double) Math.round((double) kills / deaths * 100) / 100;
    }

    public double getAccuracy() {
        return shotsFired == 0 ? 0 : (double) Math.round((double) shotsHit / shotsFired * 10000) / 100;
    }

    public double getWinRate() {
        int total = wins + losses;
        return total == 0 ? 0 : (double) Math.round((double) wins / total * 10000) / 100;
    }

    public int getTotalGames() {
        return wins + losses;
    }

    public double getKillsPerGame() {
        int games = getTotalGames();
        return games == 0 ? 0 : (double) Math.round((double) kills / games * 100) / 100;
    }
}