package drinkcounter;

import drinkcounter.authentication.PartyAccess;
import drinkcounter.model.Party;
import drinkcounter.model.PartyJoinLink;
import drinkcounter.repository.PartyJoinLinkRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Each party's one shareable join link, which any member can create, replace or revoke, and
 * with which any signed-in user can join the party until it ends.
 */
@Service
public class PartyJoinLinks {

    private static final int TOKEN_BYTES = 16;

    private final SecureRandom random = new SecureRandom();
    private final PartyJoinLinkRepository links;
    private final PartyRoster partyRoster;
    private final PartyAccess partyAccess;

    public PartyJoinLinks(PartyJoinLinkRepository links, PartyRoster partyRoster, PartyAccess partyAccess) {
        this.links = links;
        this.partyRoster = partyRoster;
        this.partyAccess = partyAccess;
    }

    public Optional<PartyJoinLink> current(int partyId) {
        return links.findById(partyId);
    }

    /** Creates the party's link, or replaces it so the old one stops working. */
    @Transactional
    public PartyJoinLink regenerate(int partyId, int byUserId) {
        PartyJoinLink link = links.findById(partyId).orElseGet(PartyJoinLink::new);
        link.setPartyId(partyId);
        link.setToken(newToken());
        link.setCreatedBy(byUserId);
        link.setCreatedAt(Instant.now());
        return links.save(link);
    }

    @Transactional
    public void revoke(int partyId) {
        links.deleteById(partyId);
    }

    @Transactional(readOnly = true)
    public JoinLinkPreview preview(String token, int userId) {
        Party party = usableParty(token);
        return new JoinLinkPreview(party, partyAccess.isMember(party.getId(), userId));
    }

    /** Adds the user to the link's party; a user who is already a member stays one. */
    @Transactional
    public Party join(String token, int userId) {
        Party party = usableParty(token);
        partyRoster.join(party.getId(), userId);
        return party;
    }

    // A party's end time can move, so a link's expiry is checked against it at each use.
    private Party usableParty(String token) {
        Party party = links.findByToken(token)
                .map(link -> partyRoster.get(link.getPartyId()))
                .orElseThrow(UnusableJoinLinkException::new);
        if (!party.getEndTime().isAfter(Instant.now())) {
            throw new UnusableJoinLinkException();
        }
        return party;
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
