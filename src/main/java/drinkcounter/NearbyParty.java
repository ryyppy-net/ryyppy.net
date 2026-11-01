package drinkcounter;

import drinkcounter.model.Party;

/** A party whose location invite reaches someone, and roughly how far away it is. */
public record NearbyParty(Party party, RoughDistance distance) {
}
