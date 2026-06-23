package me.flamboyant.manhunt.domain.game;

import java.util.UUID;

public final class GameSessionId {
    private final String value;

    public GameSessionId(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        this.value = value;
    }

    public static GameSessionId generate() {
        return new GameSessionId("session-" + UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GameSessionId that = (GameSessionId) obj;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "GameSessionId{" + value + "}";
    }
}
