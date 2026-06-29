package me.flamboyant.manhunt.domain.role.distribution;

public class InsufficientPlayersException extends RoleDistributionException {
    public InsufficientPlayersException(String message) {
        super(message);
    }
}
