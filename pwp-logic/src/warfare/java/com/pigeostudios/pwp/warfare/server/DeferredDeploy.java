package com.pigeostudios.pwp.warfare.server;

/**
 * Снимок игрока в момент, когда деплой отложен до загрузки китов. Чистая логика (без Forge):
 * при выполнении отложенного деплоя состояние игрока сравнивается со снимком — если что-то
 * поменялось, деплой отменяется, а не выполняется «вслепую».
 *
 * @param spawnType тип спавна (MAIN/RALLY/HUB...)
 * @param team команда BLUE/RED/NEUTRAL
 * @param spectator был ли игрок наблюдателем (ждал деплоя на экране выбора)
 * @param kit выбранный кит (PendingKit, иначе CurrentKit)
 */
public record DeferredDeploy(String spawnType, String team, boolean spectator, String kit) {

    /** null — деплой можно выполнять, иначе причина отмены (для сообщения игроку). */
    public String cancelReason(DeferredDeploy now) {
        if (!team.equals(now.team)) return "Команда изменена — деплой отменён";
        if (spectator && !now.spectator) return "Вы уже на поле — отложенный деплой отменён";
        if (!kit.equals(now.kit)) return "Выбор кита изменён — нажмите «Деплой» снова";
        return null;
    }
}
