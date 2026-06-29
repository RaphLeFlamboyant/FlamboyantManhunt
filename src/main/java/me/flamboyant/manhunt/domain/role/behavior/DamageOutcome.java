package me.flamboyant.manhunt.domain.role.behavior;

/**
 * Value Object representing the outcome of taking damage.
 * Encapsulates whether the entity died and the remaining health.
 */
public final class DamageOutcome {
    private final boolean died;
    private final double remainingHealth;

    private DamageOutcome(boolean died, double remainingHealth) {
        this.died = died;
        this.remainingHealth = remainingHealth;
    }

    public static DamageOutcome of(double currentHealth, double damageAmount) {
        double remaining = currentHealth - damageAmount;
        boolean died = remaining <= 0;
        return new DamageOutcome(died, remaining);
    }

    public boolean isDied() {
        return died;
    }

    public double getRemainingHealth() {
        return remainingHealth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DamageOutcome that = (DamageOutcome) o;
        return died == that.died &&
               Double.compare(that.remainingHealth, remainingHealth) == 0;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(died);
        result = 31 * result + Double.hashCode(remainingHealth);
        return result;
    }

    @Override
    public String toString() {
        return "DamageOutcome{died=" + died + ", remainingHealth=" + remainingHealth + "}";
    }
}
