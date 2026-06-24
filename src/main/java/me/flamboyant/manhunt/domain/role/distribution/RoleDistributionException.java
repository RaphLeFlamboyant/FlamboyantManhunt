package me.flamboyant.manhunt.domain.role.distribution;

public class RoleDistributionException extends RuntimeException {
    public RoleDistributionException(String message) {
        super(message);
    }

    public RoleDistributionException(String message, Throwable cause) {
        super(message, cause);
    }
}
