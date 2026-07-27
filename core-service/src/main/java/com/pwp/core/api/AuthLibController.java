package com.pwp.core.api;

import com.pwp.core.db.DatabaseManager;
import com.pwp.core.db.PlayerRepository;
import com.pwp.core.model.Player;
import io.javalin.Javalin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class BanException extends RuntimeException {
    BanException(String msg) { super(msg); }
}

public class AuthLibController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthLibController.class);

    private static final ConcurrentHashMap<String, JoinEntry> joinCache = new ConcurrentHashMap<>();
    private static final long JOIN_CACHE_TTL = 60_000;

    public AuthLibController(Javalin app) {

        // Metadata
        for (String p : new String[]{"/authlib", "/authlib/authserver"}) {
            app.get(p, ctx -> ctx.json(Map.of(
                "meta", Map.of("serverName", "PWP", "nonce", true, "enableJoinServer", true, "enableProfileKey", false),
                "skinDomains", new String[]{"pigeo.asuscomm.com"}
            )));
        }

        // Auth endpoints
        for (String base : new String[]{"/authlib", "/authlib/authserver"}) {
            app.post(base + "/authenticate", ctx -> handleAuth(ctx));
            app.post(base + "/refresh", ctx -> handleRefresh(ctx));
            app.post(base + "/validate", ctx -> handleValidate(ctx));
            app.post(base + "/signout", ctx -> ctx.status(204).result(""));
            app.post(base + "/invalidate", ctx -> ctx.status(204).result(""));
        }

        // Session endpoints (all 3 prefixes)
        for (String base : new String[]{"/authlib", "/authlib/authserver", "/authlib/sessionserver"}) {
            app.post(base + "/session/minecraft/join", ctx -> handleJoin(ctx));
            app.get(base + "/session/minecraft/hasJoined", ctx -> handleHasJoined(ctx));
            app.get(base + "/session/minecraft/profile/{uuid}", ctx -> handleProfile(ctx));
        }
    }

    private void handleAuth(io.javalin.http.Context ctx) {
        try {
            var body = ctx.bodyAsClass(AuthReq.class);
            if (body.username == null || body.password == null) { ctx.status(400).json(err("Credentials")); return; }
            String uuid = PlayerRepository.findUuidByAccessToken(body.password);
            if (uuid == null) { ctx.status(403).json(err("Invalid credentials")); return; }
            var pl = PlayerRepository.findByUuid(uuid);
            checkBan(pl);
            String ct = body.clientToken != null ? body.clientToken : UUID.randomUUID().toString().replace("-", "");
            ctx.json(Map.of(
                "accessToken", body.password, "clientToken", ct,
                "availableProfiles", java.util.List.of(mkProfile(pl.uuid, pl.nickname)),
                "selectedProfile", mkProfile(pl.uuid, pl.nickname),
                "user", Map.of("id", pl.accountId, "properties", java.util.List.of())
            ));
        } catch (BanException e) {
            ctx.status(403).json(err(e.getMessage()));
        } catch (Exception e) { ctx.status(500).json(err(e.getMessage())); }
    }

    private void handleRefresh(io.javalin.http.Context ctx) {
        try {
            var body = ctx.bodyAsClass(RefreshReq.class);
            if (body.accessToken == null) { ctx.status(400).json(err("accessToken required")); return; }
            String uuid = PlayerRepository.findUuidByAccessToken(body.accessToken);
            if (uuid == null) { ctx.status(403).json(err("Invalid token")); return; }
            var pl = PlayerRepository.findByUuid(uuid);
            checkBan(pl);
            String ct = body.clientToken != null ? body.clientToken : UUID.randomUUID().toString().replace("-", "");
            ctx.json(Map.of(
                "accessToken", body.accessToken, "clientToken", ct,
                "selectedProfile", mkProfile(pl.uuid, pl.nickname),
                "user", Map.of("id", pl.accountId, "properties", java.util.List.of())
            ));
        } catch (BanException e) {
            ctx.status(403).json(err(e.getMessage()));
        } catch (Exception e) { ctx.status(500).json(err(e.getMessage())); }
    }

    private void handleValidate(io.javalin.http.Context ctx) {
        try {
            var body = ctx.bodyAsClass(ValidateReq.class);
            if (body.accessToken == null) { ctx.status(204).result(""); return; }
            String uuid = PlayerRepository.findUuidByAccessToken(body.accessToken);
            if (uuid == null) { ctx.status(403).json(err("Invalid")); return; }
            ctx.status(204).result("");
        } catch (Exception e) { ctx.status(500).json(err(e.getMessage())); }
    }

    private void handleJoin(io.javalin.http.Context ctx) {
        try {
            var body = ctx.bodyAsClass(JoinReq.class);
            if (body.accessToken == null || body.selectedProfile == null || body.serverId == null) {
                ctx.status(400).json(err("Missing fields")); return;
            }
            String uuid = PlayerRepository.findUuidByAccessToken(body.accessToken);
            if (uuid == null) { ctx.status(403).json(err("Invalid token")); return; }
            String pu = body.selectedProfile.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            if (!pu.equalsIgnoreCase(uuid)) { ctx.status(403).json(err("UUID mismatch")); return; }
            var pl = PlayerRepository.findByUuid(uuid);
            checkBan(pl);
            joinCache.put(body.serverId, new JoinEntry(uuid, pl.nickname, System.currentTimeMillis()));
            log.info("JOIN: uuid={}, name={}, serverId={}", uuid, pl.nickname, body.serverId);
            ctx.status(204).result("");
        } catch (BanException e) {
            ctx.status(403).json(err(e.getMessage()));
        } catch (Exception e) { ctx.status(500).json(err(e.getMessage())); }
    }

    private void handleHasJoined(io.javalin.http.Context ctx) {
        try {
            String username = ctx.queryParam("username");
            String serverId = ctx.queryParam("serverId");
            if (username == null || serverId == null) { ctx.status(400).json(err("Missing params")); return; }
            log.info("HASJOINED: username={}, serverId={}", username, serverId);
            JoinEntry entry = joinCache.get(serverId);
            if (entry == null || System.currentTimeMillis() - entry.timestamp > JOIN_CACHE_TTL) {
                if (entry != null) joinCache.remove(serverId);
                log.warn("HASJOINED FAIL: not found, serverId={}", serverId);
                ctx.status(204).result(""); return;
            }
            if (!entry.username.equalsIgnoreCase(username)) {
                log.warn("HASJOINED FAIL: username mismatch (expected={}, got={})", entry.username, username);
                ctx.status(204).result(""); return;
            }
            joinCache.remove(serverId);
            log.info("HASJOINED OK: uuid={}, name={}", entry.uuid, entry.username);
            ctx.json(Map.of(
                "id", entry.uuid.replace("-", ""), "name", entry.username,
                "properties", java.util.List.of(Map.of("name", "textures", "value", buildTexture(entry.uuid, entry.username)))
            ));
        } catch (Exception e) { ctx.status(500).json(err(e.getMessage())); }
    }

    private void handleProfile(io.javalin.http.Context ctx) {
        try {
            String rawUuid = ctx.pathParam("uuid");
            String dashUuid = rawUuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            var pl = PlayerRepository.findByUuid(dashUuid);
            if (pl == null) { ctx.status(204).result(""); return; }
            ctx.json(Map.of(
                "id", rawUuid, "name", pl.nickname,
                "properties", java.util.List.of(Map.of("name", "textures", "value", buildTexture(pl.uuid, pl.nickname)))
            ));
        } catch (Exception e) { ctx.status(500).json(err(e.getMessage())); }
    }

    private Map<String, Object> mkProfile(String uuid, String name) {
        return Map.of("id", uuid.replace("-", ""), "name", name);
    }

    private String buildTexture(String uuid, String name) {
        try {
            String json = "{\"timestamp\":" + System.currentTimeMillis()
                + ",\"profileId\":\"" + uuid.replace("-", "") + "\""
                + ",\"profileName\":\"" + name + "\""
                + ",\"textures\":{\"SKIN\":{\"url\":\"https://pigeo.asuscomm.com/authlib/textures/steve.png\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) { return ""; }
    }

    private static boolean isHwidBanned(String hwid) {
        String sql = "SELECT banned_until FROM hwid_bans WHERE hwid = ? LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hwid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                java.sql.Timestamp until = rs.getTimestamp("banned_until");
                if (until != null && until.before(new java.util.Date())) {
                    try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM hwid_bans WHERE hwid = ?")) {
                        del.setString(1, hwid);
                        del.executeUpdate();
                    }
                    return false;
                }
                return true;
            }
        } catch (Exception e) { return false; }
    }

    private void checkBan(Player pl) {
        if (pl == null || pl.isBanned) throw new BanException("Banned");
        if (pl.hwid != null && !pl.hwid.isEmpty() && isHwidBanned(pl.hwid))
            throw new BanException("Banned");
    }

    private Map<String, Object> err(String msg) {
        return Map.of("error", msg, "errorCode", 403, "cause", msg);
    }

    private static class JoinEntry {
        final String uuid; final String username; final long timestamp;
        JoinEntry(String u, String n, long t) { this.uuid = u; this.username = n; this.timestamp = t; }
    }

    private static class AuthReq {
        public String username; public String password; public String clientToken; public boolean requestUser;
    }
    private static class RefreshReq {
        public String accessToken; public String clientToken; public boolean requestUser;
    }
    private static class ValidateReq {
        public String accessToken; public String clientToken;
    }
    private static class JoinReq {
        public String accessToken; public String selectedProfile; public String serverId;
    }
}
