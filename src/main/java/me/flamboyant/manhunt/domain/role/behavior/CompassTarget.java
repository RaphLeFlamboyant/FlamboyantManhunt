package me.flamboyant.manhunt.domain.role.behavior;

import org.bukkit.Location;

/**
 * Value Object representing a compass tracking target.
 * Encapsulates the target location and whether it's cross-dimension.
 */
public final class CompassTarget {
    private final Location location;
    private final boolean crossDimension;
    private final String targetDimensionName;

    private CompassTarget(Location location, boolean crossDimension, String targetDimensionName) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        this.location = location;
        this.crossDimension = crossDimension;
        this.targetDimensionName = targetDimensionName;
    }

    public static CompassTarget sameDimension(Location location) {
        return new CompassTarget(location, false, null);
    }

    public static CompassTarget crossDimension(Location portalLocation, String targetDimensionName) {
        return new CompassTarget(portalLocation, true, targetDimensionName);
    }

    public Location getLocation() {
        return location;
    }

    public boolean isCrossDimension() {
        return crossDimension;
    }

    public String getTargetDimensionName() {
        return targetDimensionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompassTarget that = (CompassTarget) o;
        return crossDimension == that.crossDimension &&
               location.equals(that.location) &&
               (targetDimensionName == null ? that.targetDimensionName == null :
                targetDimensionName.equals(that.targetDimensionName));
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + Boolean.hashCode(crossDimension);
        result = 31 * result + (targetDimensionName != null ? targetDimensionName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CompassTarget{location=" + location +
               ", crossDimension=" + crossDimension +
               ", targetDimensionName='" + targetDimensionName + "'}";
    }
}
