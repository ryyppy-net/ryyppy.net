package drinkcounter.web.controllers.api.v2;

import drinkcounter.InvalidPositionException;
import drinkcounter.NearbyParties;
import drinkcounter.Position;
import drinkcounter.UnusableLocationInviteException;
import drinkcounter.authentication.LoggedInUserId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Any signed-in user can find the parties whose location invite reaches them, preview one and
 * join it. The caller's coordinates are used for the distance check only: not stored, not logged.
 */
@RestController
@RequestMapping("API/v2/nearby-parties")
public class NearbyPartyApiController {

    private final NearbyParties nearbyParties;

    public NearbyPartyApiController(NearbyParties nearbyParties) {
        this.nearbyParties = nearbyParties;
    }

    @GetMapping
    public List<NearbyPartyDTO> search(@RequestParam double lat, @RequestParam double lon) {
        return nearbyParties.around(new Position(lat, lon)).stream().map(NearbyPartyDTO::of).toList();
    }

    @GetMapping("{partyId}")
    public NearbyPartyPreviewDTO preview(@PathVariable int partyId, @RequestParam double lat,
            @RequestParam double lon, @LoggedInUserId int userId) {
        return NearbyPartyPreviewDTO.of(nearbyParties.preview(partyId, new Position(lat, lon), userId));
    }

    @PostMapping("{partyId}/participants")
    public PartyDTO join(@PathVariable int partyId, @RequestBody PositionRequest request,
            @LoggedInUserId int userId) {
        if (request.latitude() == null || request.longitude() == null) {
            throw new InvalidPositionException();
        }
        Position position = new Position(request.latitude(), request.longitude());
        return PartyDTO.fromParty(nearbyParties.join(partyId, position, userId));
    }

    // One answer for every party that cannot be joined from here, so none can be told apart.
    @ExceptionHandler(UnusableLocationInviteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void unusableInvite() {
    }

    // Handled here so Spring's default resolver does not log the rejected coordinate values.
    @ExceptionHandler({InvalidPositionException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void invalidPosition() {
    }
}
