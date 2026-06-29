package me.flamboyant.manhunt.domain.lifecycle;

/**
 * Tracks the completion status of compensation steps during game start failure.
 *
 * Used by GameStartFailedEvent to communicate which cleanup steps were successfully
 * performed during rollback. This allows handlers to know what state was reverted.
 */
public class CompensationStatus {
    private boolean handlersUnregistered;
    private boolean rolesCleared;
    private boolean sessionDeleted;

    public CompensationStatus() {
        this.handlersUnregistered = false;
        this.rolesCleared = false;
        this.sessionDeleted = false;
    }

    public void markHandlersUnregistered() {
        this.handlersUnregistered = true;
    }

    public void markRolesCleared() {
        this.rolesCleared = true;
    }

    public void markSessionDeleted() {
        this.sessionDeleted = true;
    }

    public boolean isHandlersUnregistered() {
        return handlersUnregistered;
    }

    public boolean isRolesCleared() {
        return rolesCleared;
    }

    public boolean isSessionDeleted() {
        return sessionDeleted;
    }

    public boolean isFullyCompensated() {
        return handlersUnregistered && rolesCleared && sessionDeleted;
    }
}
