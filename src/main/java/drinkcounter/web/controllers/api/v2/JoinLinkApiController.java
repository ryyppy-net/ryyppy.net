package drinkcounter.web.controllers.api.v2;

import drinkcounter.PartyJoinLinks;
import drinkcounter.UnusableJoinLinkException;
import drinkcounter.authentication.LoggedInUserId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Any signed-in user holding a usable join link can preview its party and join it. */
@RestController
@RequestMapping("API/v2/join-links/{token}")
public class JoinLinkApiController {

    private final PartyJoinLinks joinLinks;

    public JoinLinkApiController(PartyJoinLinks joinLinks) {
        this.joinLinks = joinLinks;
    }

    @GetMapping
    public JoinLinkPreviewDTO preview(@PathVariable String token, @LoggedInUserId int userId) {
        return JoinLinkPreviewDTO.of(joinLinks.preview(token, userId));
    }

    @PostMapping("participants")
    public PartyDTO join(@PathVariable String token, @LoggedInUserId int userId) {
        return PartyDTO.fromParty(joinLinks.join(token, userId));
    }

    // One answer for every unusable link, so it cannot be told whether a token ever existed.
    @ExceptionHandler(UnusableJoinLinkException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void unusableLink() {
    }
}
