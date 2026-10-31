package drinkcounter;

import drinkcounter.model.Party;

/** The party a join link leads to, as the user holding the link sees it before joining. */
public record JoinLinkPreview(Party party, boolean alreadyMember) {
}
