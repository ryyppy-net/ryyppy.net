package drinkcounter.web.controllers.api.v2;

import drinkcounter.PartyJoinLinks;
import drinkcounter.authentication.LoggedInUserId;
import drinkcounter.authentication.PartyMember;
import drinkcounter.authentication.relay.Origins;
import drinkcounter.model.PartyJoinLink;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("API/v2/parties/{partyId}/join-link")
public class PartyJoinLinkApiController {

    private final PartyJoinLinks joinLinks;

    public PartyJoinLinkApiController(PartyJoinLinks joinLinks) {
        this.joinLinks = joinLinks;
    }

    @GetMapping
    @PartyMember
    public ResponseEntity<JoinLinkDTO> getJoinLink(@PathVariable Integer partyId, HttpServletRequest request) {
        return ResponseEntity.of(joinLinks.current(partyId).map(link -> toDTO(link, request)));
    }

    /** Creates the link, or replaces the current one so it stops working. */
    @PostMapping
    @PartyMember
    public JoinLinkDTO createJoinLink(@PathVariable Integer partyId, @LoggedInUserId int userId,
            HttpServletRequest request) {
        return toDTO(joinLinks.regenerate(partyId, userId), request);
    }

    @DeleteMapping
    @PartyMember
    public ResponseEntity<Void> revokeJoinLink(@PathVariable Integer partyId) {
        joinLinks.revoke(partyId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // A PR preview hands out links to its own domain, not to production's.
    private static JoinLinkDTO toDTO(PartyJoinLink link, HttpServletRequest request) {
        return new JoinLinkDTO(Origins.of(request) + "/join/" + link.getToken(), Date.from(link.getCreatedAt()));
    }
}
