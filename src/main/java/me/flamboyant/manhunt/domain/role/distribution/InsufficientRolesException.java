package me.flamboyant.manhunt.domain.role.distribution;

public class InsufficientRolesException extends RoleDistributionException {
    public InsufficientRolesException(String message) {
        super(message);
    }
}
