package drinkcounter.web.controllers.api.v2;

public record LocationInviteRequest(Double latitude, Double longitude, Integer radiusM) {
}
