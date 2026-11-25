package com.pwp.coreclient.admin;

import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.DataRequest;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Слой «источник данных» админ-панели: вкладки шлют запросы и действия ТОЛЬКО через него. В игре —
 * {@link #NETWORK} (DataRequest + пакеты своих каналов), в тест-стенде UI — {@link #PREVIEW} (ничего не
 * уходит в сеть, только лог; данные вкладки берут из своих фикстур).
 */
@OnlyIn(Dist.CLIENT)
public interface AdminBackend {
    org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AdminBackend.class);

    /** Запрос данных GUI (тип {@code DataRequests}). */
    void request(String type, String paramsJson);

    /**
     * Админ-действие: {@code send} отправляет пакет своего канала (warfare/core-client). В превью не
     * вызывается — пишется лишь {@code actionId}.
     */
    void perform(String actionId, Runnable send);

    boolean preview();

    AdminBackend NETWORK = new AdminBackend() {
        @Override
        public void request(String type, String paramsJson) {
            PacketHandler.INSTANCE.sendToServer(new DataRequest(type, paramsJson == null ? "" : paramsJson));
        }

        @Override
        public void perform(String actionId, Runnable send) {
            send.run();
        }

        @Override
        public boolean preview() {
            return false;
        }
    };

    AdminBackend PREVIEW = new AdminBackend() {
        @Override
        public void request(String type, String paramsJson) {
            LOG.debug("[PWPUI] admin preview: запрос {} не отправлен", type);
        }

        @Override
        public void perform(String actionId, Runnable send) {
            LOG.info("[PWPUI] admin preview: действие {} не отправлено", actionId);
        }

        @Override
        public boolean preview() {
            return true;
        }
    };
}
