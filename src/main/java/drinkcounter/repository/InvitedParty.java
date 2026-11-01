package drinkcounter.repository;

import drinkcounter.model.Party;
import drinkcounter.model.PartyLocationInvite;

public record InvitedParty(PartyLocationInvite invite, Party party) {
}
