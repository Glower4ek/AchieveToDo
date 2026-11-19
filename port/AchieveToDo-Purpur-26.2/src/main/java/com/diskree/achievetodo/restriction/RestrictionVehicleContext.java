package com.diskree.achievetodo.restriction;

import java.util.Objects;
import java.util.UUID;

public record RestrictionVehicleContext(
    UUID vehicleId,
    String vehicleTypeKey,
    boolean controllingPassenger,
    int passengerCount
) {

    public RestrictionVehicleContext {
        vehicleTypeKey = normalize(vehicleTypeKey);
        if (passengerCount < 0) {
            throw new IllegalArgumentException("passengerCount must not be negative");
        }
    }

    public static RestrictionVehicleContext none() {
        return new RestrictionVehicleContext(null, null, false, 0);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
