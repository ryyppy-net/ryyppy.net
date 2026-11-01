package drinkcounter.web.controllers.api.v2;

import drinkcounter.InvalidLocationInviteException;
import drinkcounter.PartyLocationInvites;
import drinkcounter.authentication.LoggedInUserId;
import drinkcounter.authentication.PartyMember;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("API/v2/parties/{partyId}/location-invite")
public class PartyLocationInviteApiController {

    private final PartyLocationInvites locationInvites;

    public PartyLocationInviteApiController(PartyLocationInvites locationInvites) {
        this.locationInvites = locationInvites;
    }

    @GetMapping
    @PartyMember
    public ResponseEntity<LocationInviteDTO> getLocationInvite(@PathVariable Integer partyId) {
        return ResponseEntity.of(locationInvites.current(partyId).map(LocationInviteDTO::of));
    }

    /** Turns the invite on, or replaces its place and radius. */
    @PutMapping
    @PartyMember
    public LocationInviteDTO setLocationInvite(@PathVariable Integer partyId, @LoggedInUserId int userId,
            @RequestBody LocationInviteRequest request) {
        if (request.latitude() == null || request.longitude() == null || request.radiusM() == null) {
            throw new InvalidLocationInviteException("latitude, longitude and radiusM are required");
        }
        PartyLocationInvites.requireValid(request.latitude(), request.longitude(), request.radiusM());
        return LocationInviteDTO.of(locationInvites.set(partyId, request.latitude(), request.longitude(),
                request.radiusM(), userId));
    }

    @DeleteMapping
    @PartyMember
    public ResponseEntity<Void> revokeLocationInvite(@PathVariable Integer partyId) {
        locationInvites.revoke(partyId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @ExceptionHandler(InvalidLocationInviteException.class)
    public ResponseEntity<String> handleInvalidInvite(InvalidLocationInviteException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
