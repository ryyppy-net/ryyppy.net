package drinkcounter;

/** A nearby party as the user who found it sees it before joining. */
public record NearbyPartyPreview(NearbyParty nearby, boolean alreadyMember) {
}
