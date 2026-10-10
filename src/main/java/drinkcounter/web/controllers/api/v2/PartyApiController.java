package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.DrinkLog;
import drinkcounter.UserAccounts;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.authentication.LoggedInUserId;
import drinkcounter.authentication.PartyMember;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Toni
 */
@RestController
@RequestMapping("API/v2/parties")
public class PartyApiController {

    private final DrinkCounterService drinkCounterService;
    private final DrinkLog drinkLog;
    private final UserAccounts userAccounts;

    public PartyApiController(DrinkCounterService drinkCounterService, DrinkLog drinkLog, UserAccounts userAccounts) {
        this.drinkCounterService = drinkCounterService;
        this.drinkLog = drinkLog;
        this.userAccounts = userAccounts;
    }

    @GetMapping
    public List<PartyDTO> getParties(@LoggedInUser User user){
        List<Party> parties = user.getParties();
        List<PartyDTO> partyDTOs = new ArrayList<PartyDTO>();
        for (Party party : parties) {
            PartyDTO partyDTO = PartyDTO.fromParty(party);
            List<User> participants = party.getParticipants();
            for (User participant : participants) {
                partyDTO.addParticipant(ParticipantPreviewDTO.fromUser(participant));
            }
            partyDTOs.add(partyDTO);
        }
        return partyDTOs;
    }
    
    @PostMapping
    public PartyDTO addParty(@LoggedInUserId int userId, @RequestParam("name") String partyName){
        Party party = drinkCounterService.startParty(partyName);
        drinkCounterService.linkUserToParty(userId, party.getId());
        return PartyDTO.fromParty(party);
    }

    @GetMapping("{partyId}")
    @PartyMember
    public PartyDTO getParty(@PathVariable Integer partyId){
        Party party = drinkCounterService.getParty(partyId);
        return PartyDTO.fromParty(party);
    }

    @GetMapping("{partyId}/participants")
    @PartyMember
    public List<ParticipantDTO> getParticipants(@PathVariable Integer partyId){
        Party party = drinkCounterService.getParty(partyId);
        List<User> participants = party.getParticipants();
        List<ParticipantDTO> participantDTOs = new ArrayList<ParticipantDTO>();
        for (User participant : participants) {
            ParticipantDTO participantDTO = ParticipantDTO.fromUser(participant);
            
            List<HistoryPoint> history = PromilleHistory.forUser(participant);
            participantDTO.setHistory(history);
            
            participantDTOs.add(participantDTO);
        }
        return participantDTOs;
    }

    @PostMapping("{partyId}/participants")
    @PartyMember
    public void addParticipant(@PathVariable Integer partyId, @RequestParam(value="email", required=false) String email,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="sex", required=false) User.Sex sex,
            @RequestParam(value="weight", required=false) Float weight){
        if(email != null){
            User user = userAccounts.byEmail(email);
            drinkCounterService.linkUserToParty(user.getId(), partyId);
            return;
        }
        
        if (name == null || sex == null || weight == null) {
            throw new RuntimeException("If email is not provided, give name, sex and weight to create a guest participant");
        }
        
        User user = new User();
        user.setName(name);
        user.setSex(sex);
        user.setWeight(weight);
        user.setGuest(true);
        userAccounts.add(user);
        drinkCounterService.linkUserToParty(user.getId(), partyId);
    }

    @DeleteMapping("{partyId}/participants/{participantId}")
    @PartyMember
    public void removeParticipant(@PathVariable Integer partyId, @PathVariable Integer participantId){
        drinkCounterService.unlinkUserFromParty(participantId, partyId);
    }

    @GetMapping("{partyId}/participants/{participantId}")
    @PartyMember
    public ParticipantDTO getParticipant(@PathVariable Integer partyId, @PathVariable Integer participantId){
        return ParticipantDTO.fromUser(requireParticipant(partyId, participantId));
    }

    @PostMapping("{partyId}/participants/{participantId}/drinks")
    @PartyMember
    public DrinkDTO drink(@PathVariable Integer partyId, @PathVariable Integer participantId,
            @RequestParam(value="volume", required=false) Float volume,
            @RequestParam(value="alcohol", required=false) Float alcoholPercentage,
            @RequestParam(value="timestamp", required=false) String timestamp){
        requireParticipant(partyId, participantId);
        float alcoholAmount = (float)AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS;
        if (volume != null && alcoholPercentage != null) {
            alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
        }
        Date time = null;
        if(timestamp != null){
            time = Date.from(Instant.parse(timestamp));
        }
        return DrinkDTO.fromDrink(drinkLog.record(participantId, time, alcoholAmount));
    }

    @PutMapping("{partyId}/participants/{participantId}/drinks/{drinkId}")
    @PartyMember
    public void changeDrink(@PathVariable Integer partyId, @PathVariable Integer participantId,
            @PathVariable Integer drinkId,
            @RequestParam("volume") Float volume,
            @RequestParam("alcohol") Float alcoholPercentage){
        requireParticipant(partyId, participantId);
        drinkLog.correctAlcohol(participantId, drinkId,
                AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage));
    }

    @DeleteMapping("{partyId}/participants/{participantId}/drinks/{drinkId}")
    @PartyMember
    public void removeDrink(@PathVariable Integer partyId, @PathVariable Integer participantId,
            @PathVariable Integer drinkId){
        requireParticipant(partyId, participantId);
        drinkLog.undo(participantId, drinkId);
    }

    private User requireParticipant(Integer partyId, Integer participantId) {
        Party party = drinkCounterService.getParty(partyId);
        User participant = userAccounts.get(participantId);
        if(!party.getParticipants().contains(participant)){
            throw new RuntimeException(MessageFormat.format("Participant {0} doesn''t belong to party {1}", participant.getId(), party.getId()));
        }
        return participant;
    }

    @GetMapping("{partyId}/invitations")
    @PartyMember
    public List<Friend> suggestInvite(@LoggedInUserId int userId, @PathVariable Integer partyId, @RequestParam(defaultValue = "10", value="amount") int amount){
        return drinkCounterService.suggestInvitations(userId, partyId, amount);
    }

    @PostMapping("{partyId}/invitations")
    @PartyMember
    public void invitePerson(@PathVariable Integer partyId, @RequestParam(value="userId") int userId){
        drinkCounterService.linkUserToParty(userId, partyId);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<String> handleInvalidTimestamp(DateTimeParseException ex) {
        return ResponseEntity.badRequest().body("Invalid timestamp: " + ex.getMessage());
    }
}
