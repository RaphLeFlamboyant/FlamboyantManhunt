package me.flamboyant.manhunt.domain.event;

import me.flamboyant.manhunt.domain.game.GameSessionId;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameSessionCreatedEvent extends DomainEvent {
    private final List<Player> players;

    public GameSessionCreatedEvent(GameSessionId sessionId, List<Player> players) {
        super(sessionId);
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
    }

    public List<Player> getPlayers() {
        return players;
    }
}
