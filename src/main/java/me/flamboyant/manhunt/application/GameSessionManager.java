package me.flamboyant.manhunt.application;

import me.flamboyant.manhunt.domain.game.GameSession;
import me.flamboyant.manhunt.domain.game.GameSessionId;
import me.flamboyant.manhunt.domain.tracking.InMemoryPortalTracker;
import org.bukkit.entity.Player;

import java.util.*;

public class GameSessionManager {
    private static GameSessionManager instance;

    private final Map<GameSessionId, GameSession> sessions;

    private GameSessionManager() {
        this.sessions = new HashMap<>();
    }

    public static GameSessionManager getInstance() {
        if (instance == null) {
            instance = new GameSessionManager();
        }
        return instance;
    }

    public GameSession createSession() {
        GameSessionId id = GameSessionId.generate();
        return createSession(id);
    }

    public GameSession createSession(GameSessionId id) {
        if (sessions.containsKey(id)) {
            throw new IllegalArgumentException("Session with ID " + id + " already exists");
        }

        GameSession session = new GameSession(id);
        sessions.put(id, session);
        return session;
    }

    public GameSession getSession(GameSessionId id) {
        return sessions.get(id);
    }

    public GameSession getActiveSessionForPlayer(Player player) {
        for (GameSession session : sessions.values()) {
            if (session.hasRole(player)) {
                return session;
            }
        }
        return null;
    }

    public Set<GameSession> getAllSessions() {
        return Collections.unmodifiableSet(new HashSet<>(sessions.values()));
    }

    public void removeSession(GameSessionId id) {
        GameSession session = sessions.remove(id);
        if (session != null) {
            session.clear();
        }
    }

    public void removeAllSessions() {
        for (GameSession session : sessions.values()) {
            session.clear();
        }
        sessions.clear();
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}
