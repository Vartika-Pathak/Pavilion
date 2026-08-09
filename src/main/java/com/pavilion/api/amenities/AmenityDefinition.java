package com.pavilion.api.amenities;

public record AmenityDefinition(String id, String name, String description, boolean requiresPayment, int priceCents) {
}
