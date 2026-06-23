package me.flamboyant.manhunt.application.exceptions;

import org.junit.Test;

import static org.junit.Assert.*;

public class CompensationStatusTest {

    @Test
    public void newStatus_shouldNotBeFullyCompensated() {
        CompensationStatus status = new CompensationStatus();

        assertFalse(status.isFullyCompensated());
        assertFalse(status.isHandlersUnregistered());
        assertFalse(status.isRolesCleared());
        assertFalse(status.isSessionDeleted());
    }

    @Test
    public void markHandlersUnregistered_shouldUpdateStatus() {
        CompensationStatus status = new CompensationStatus();

        status.markHandlersUnregistered();

        assertTrue(status.isHandlersUnregistered());
        assertFalse(status.isFullyCompensated());
    }

    @Test
    public void markAllSteps_shouldBeFullyCompensated() {
        CompensationStatus status = new CompensationStatus();

        status.markHandlersUnregistered();
        status.markRolesCleared();
        status.markSessionDeleted();

        assertTrue(status.isFullyCompensated());
    }
}
