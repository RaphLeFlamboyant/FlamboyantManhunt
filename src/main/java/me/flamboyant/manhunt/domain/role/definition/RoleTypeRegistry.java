package me.flamboyant.manhunt.domain.role.definition;

import java.util.*;
import java.util.stream.Collectors;

public final class RoleTypeRegistry {
    private static final Map<ManhuntRoleType, List<ManhuntRoleIdentifier>> BY_TYPE;

    static {
        BY_TYPE = Arrays.stream(ManhuntRoleIdentifier.values())
            .collect(Collectors.groupingBy(
                ManhuntRoleIdentifier::getRoleType,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    Collections::unmodifiableList
                )
            ));
    }

    private RoleTypeRegistry() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Get all role identifiers of a specific type.
     *
     * @param type the role type to query
     * @return immutable list of matching role identifiers (empty if none)
     */
    public static List<ManhuntRoleIdentifier> getRolesByType(ManhuntRoleType type) {
        return BY_TYPE.getOrDefault(type, Collections.emptyList());
    }

    /**
     * Get all role identifiers of a specific type, excluding specified roles.
     *
     * @param type the role type to query
     * @param exclusions role identifiers to exclude from results
     * @return immutable list of matching role identifiers
     */
    public static List<ManhuntRoleIdentifier> getRolesByTypeExcluding(
            ManhuntRoleType type,
            ManhuntRoleIdentifier... exclusions) {
        Set<ManhuntRoleIdentifier> excludeSet = new HashSet<>(Arrays.asList(exclusions));
        return Collections.unmodifiableList(
            BY_TYPE.getOrDefault(type, Collections.emptyList())
                .stream()
                .filter(id -> !excludeSet.contains(id))
                .collect(Collectors.toList())
        );
    }
}
