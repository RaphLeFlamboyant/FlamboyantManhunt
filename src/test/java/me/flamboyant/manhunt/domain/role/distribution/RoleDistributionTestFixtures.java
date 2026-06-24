package me.flamboyant.manhunt.domain.role.distribution;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.mockito.Mockito.*;

public class RoleDistributionTestFixtures {
    public static List<Player> createMockPlayers(int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Player player = mock(Player.class);
            when(player.getName()).thenReturn("Player" + (i + 1));
            when(player.getDisplayName()).thenReturn("Player" + (i + 1));
            players.add(player);
        }
        return players;
    }

    public static RoleDistributionConfig balancedConfig() {
        return RoleDistributionConfig.balanced();
    }

    public static RoleDistributionConfig specialOnlyConfig() {
        return RoleDistributionConfig.specialOnly();
    }

    public static Random seededRandom(long seed) {
        return new Random(seed);
    }
}
