package com.pwp.coreserver;

import net.minecraft.server.level.ServerPlayer;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Выбор адреса для переброса игроков между лобби и матч-серверами.
 * <p>
 * Локальные игроки (та же LAN) получают LAN-адрес серверной машины, чтобы не ходить
 * через NAT-loopback роутера: роутер теряет обратные пакеты для новых соединений
 * на недавно использованный WAN-порт, из-за чего повторный вход в матч зависал
 * («превышено ожидание»). Внешние игроки получают публичный домен.
 */
public final class TransferHosts {

    /** Публичный адрес серверов (DDNS). */
    public static final String PUBLIC_HOST = "pigeo.asuscomm.com";

    /** Резервный LAN-адрес серверной машины, если автоопределение не сработало. */
    private static final String FALLBACK_LAN_HOST = "192.168.50.249";

    private static volatile String lanHost = null;

    private TransferHosts() {
    }

    /** LAN-адрес серверной машины (автоопределение, результат кэшируется). */
    public static String getLanHost() {
        if (lanHost != null) {
            return lanHost;
        }
        String detected = detectLanHost();
        lanHost = detected != null ? detected : FALLBACK_LAN_HOST;
        return lanHost;
    }

    /**
     * Хост для переброса игрока: приватный IP игрока -> LAN-адрес сервера,
     * публичный IP -> домен.
     */
    public static String resolveTransferHost(ServerPlayer player) {
        String ip = null;
        try {
            ip = player.getIpAddress();
        } catch (Exception ignored) {
        }
        if (isPrivateIp(ip)) {
            return getLanHost();
        }
        return PUBLIC_HOST;
    }

    private static String detectLanHost() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (!(a instanceof Inet4Address)) continue;
                    String h = a.getHostAddress();
                    if (h != null && isPrivateIp(h) && !h.startsWith("127.")) {
                        return h;
                    }
                }
            }
        } catch (Exception e) {
            CoreServerMod.log.warn("TransferHosts: не удалось определить LAN-адрес: {}", e.getMessage());
        }
        return null;
    }

    private static boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if (ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            int dot2 = ip.indexOf('.', 4);
            if (dot2 > 4) {
                try {
                    int second = Integer.parseInt(ip.substring(4, dot2));
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }
}
