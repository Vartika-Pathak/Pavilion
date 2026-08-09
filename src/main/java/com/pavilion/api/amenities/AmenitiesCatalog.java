package com.pavilion.api.amenities;

import java.util.List;
import java.util.Optional;

// A fixed, code-defined catalog rather than a database table — matches the Node original,
// which never gave amenities their own admin-managed table either.
public class AmenitiesCatalog {

    public static final List<AmenityDefinition> CATALOG = List.of(
            new AmenityDefinition("clubhouse", "Clubhouse", "Shared lounge and event space.", false, 0),
            new AmenityDefinition("swimming_pool", "Swimming Pool", "Residents' pool.", false, 0),
            new AmenityDefinition("tennis_court", "Tennis Court", "Outdoor court, 2-hour sessions.", true, 1000),
            new AmenityDefinition("party_hall", "Party Hall", "Private hall for events, catering allowed.", true, 5000));

    private AmenitiesCatalog() {
    }

    public static Optional<AmenityDefinition> find(String id) {
        return CATALOG.stream().filter(a -> a.id().equals(id)).findFirst();
    }
}
