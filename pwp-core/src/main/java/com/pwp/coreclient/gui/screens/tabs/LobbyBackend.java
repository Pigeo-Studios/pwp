package com.pwp.coreclient.gui.screens.tabs;

import com.pwp.coreclient.network.JoinMatchServerPacket;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.DataRequest;
import net.minecraft.client.Minecraft;

/**
 * Действия лобби, уходящие на сервер (голос, вход в матч, запросы данных GUI). Вкладки зовут
 * только этот интерфейс: в игре — {@link #NETWORK} (те же команды/пакеты, что и раньше), в
 * тест-стенде UI — {@link #PREVIEW} (ничего не отправляет, только пишет в лог).
 */
public interface LobbyBackend {
    org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LobbyBackend.class);

    /** Серверная команда без «/» (votemap, votemode, votefaction, pwp). */
    void sendCommand(String command);

    void joinMatch(int serverId);

    void requestLeaderboard(String orderBy, int page);

    void requestProfile();

    /** Реальная сеть — поведение прежнее. */
    LobbyBackend NETWORK = new LobbyBackend() {
        @Override
        public void sendCommand(String command) {
            var player = Minecraft.getInstance().player;
            if (player == null || player.connection == null) {
                LOG.warn("LobbyBackend.sendCommand: нет соединения, команда «{}» не отправлена", command);
                return;
            }
            player.connection.sendCommand(command);
        }

        @Override
        public void joinMatch(int serverId) {
            PacketHandler.INSTANCE.sendToServer(new JoinMatchServerPacket(serverId));
        }

        @Override
        public void requestLeaderboard(String orderBy, int page) {
            PacketHandler.INSTANCE.sendToServer(new DataRequest(
                    "leaderboard", "{\"orderBy\":\"" + orderBy + "\",\"page\":" + page + ",\"limit\":100}"));
        }

        @Override
        public void requestProfile() {
            PacketHandler.INSTANCE.sendToServer(new DataRequest("profile", ""));
        }
    };

    /** Тест-стенд: ничего не уходит в сеть. */
    LobbyBackend PREVIEW = new LobbyBackend() {
        @Override
        public void sendCommand(String command) {
            LOG.info("[PWPUI] preview: команда «{}» не отправлена", command);
        }

        @Override
        public void joinMatch(int serverId) {
            LOG.info("[PWPUI] preview: вход в матч {} не отправлен", serverId);
        }

        @Override
        public void requestLeaderboard(String orderBy, int page) {}

        @Override
        public void requestProfile() {}
    };
}
