package drinkcounter;

import drinkcounter.model.PartyJoinLink;
import drinkcounter.repository.PartyJoinLinkRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Each party's one shareable join link, which any member can create, replace or revoke. */
@Service
public class PartyJoinLinks {

    private static final int TOKEN_BYTES = 16;

    private final SecureRandom random = new SecureRandom();
    private final PartyJoinLinkRepository links;

    public PartyJoinLinks(PartyJoinLinkRepository links) {
        this.links = links;
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

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
