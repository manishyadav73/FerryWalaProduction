package backend.demand;

import backend.model.Category;
import backend.model.SignalType;

import java.time.Instant;

/** One piece of demand evidence, decoupled from persistence so the
 *  scoring engine is plain Java and unit-testable without Spring. */
public record SignalInput(
        SignalType type,
        String productName,
        Category category,
        double latitude,
        double longitude,
        String areaKey,
        String areaName,
        Instant at,
        Long customerId
) {
}
