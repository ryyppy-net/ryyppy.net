package drinkcounter.web.controllers.api.v2;

import drinkcounter.InvitationSuggestions;
import drinkcounter.PartyRoster;
import drinkcounter.DrinkLog;
import drinkcounter.Promilles;
import drinkcounter.UserAccounts;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.authentication.LoggedInUserId;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.authentication.PartyMember;
import drinkcounter.model.Drink;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Toni
 */
@RestController
@RequestMapping("API/v2/parties")
public class PartyApiController {

    private final PartyRoster partyRoster;
    private final InvitationSuggestions invitationSuggestions;
    private final DrinkLog drinkLog;
    private final UserAccounts userAccounts;
    private final PartyAccess partyAccess;
    private final Promilles promilles;

    public PartyApiController(PartyRoster partyRoster, InvitationSuggestions invitationSuggestions,
            DrinkLog drinkLog, UserAccounts userAccounts, PartyAccess partyAccess, Promilles promilles) {
        this.partyRoster = partyRoster;
        this.invitationSuggestions = invitationSuggestions;
        this.drinkLog = drinkLog;
        this.userAccounts = userAccounts;
        this.partyAccess = partyAccess;
        this.promilles = promilles;
    }

    /** The user's parties; {@code limit} keeps only that many of the newest, newest first. */
    @GetMapping
    public List<PartyDTO> getParties(@LoggedInUser User user,
            @RequestParam(value="limit", required=false) Integer limit){
        List<Party> parties = limit == null ? user.getParties() : partyRoster.newestWithParticipantsOf(user.getId(), Math.max(1, limit));
        List<PartyDTO> partyDTOs = new ArrayList<PartyDTO>();
        for (Party party : parties) {
            PartyDTO partyDTO = PartyDTO.fromParty(party);
            Set<User> participants = party.getParticipants();
            for (User participant : participants) {
                partyDTO.addParticipant(ParticipantPreviewDTO.fromUser(participant));
            }
            partyDTOs.add(partyDTO);
        }
        return partyDTOs;
    }
    
    @PostMapping
    public PartyDTO addParty(@LoggedInUserId int userId, @RequestParam("name") String partyName){
        Party party = partyRoster.start(partyName);
        partyRoster.join(party.getId(), userId);
        return PartyDTO.fromParty(party);
    }

    @GetMapping("{partyId}")
    @PartyMember
    public PartyDTO getParty(@PathVariable Integer partyId){
        Party party = partyRoster.get(partyId);
        return PartyDTO.fromParty(party);
    }

    @GetMapping("{partyId}/participants")
    @PartyMember
    public List<ParticipantDTO> getParticipants(@PathVariable Integer partyId){
        Party party = partyRoster.get(partyId);
        Set<User> participants = party.getParticipants();
        List<ParticipantDTO> participantDTOs = new ArrayList<ParticipantDTO>();
        for (BloodAlcohol participant : promilles.of(participants)) {
            ParticipantDTO participantDTO = ParticipantDTO.fromUser(participant);
            
            List<HistoryPoint> history = PromilleHistory.of(participant);
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
            partyRoster.join(partyId, user.getId());
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
        partyRoster.join(partyId, user.getId());
    }

    @DeleteMapping("{partyId}/participants/{participantId}")
    @PartyMember
    public void removeParticipant(@PathVariable Integer partyId, @PathVariable Integer participantId){
        partyRoster.leave(partyId, participantId);
    }

    @GetMapping("{partyId}/participants/{participantId}")
    @PartyMember
    public ParticipantDTO getParticipant(@PathVariable Integer partyId, @PathVariable Integer participantId){
        requireParticipant(partyId, participantId);
        return ParticipantDTO.fromUser(promilles.of(userAccounts.get(participantId)));
    }

    @PostMapping("{partyId}/participants/{participantId}/drinks")
    @PartyMember
    public ParticipantDrinksDTO drink(@PathVariable Integer partyId, @PathVariable Integer participantId,
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
        Drink drink = drinkLog.record(participantId, time, alcoholAmount);
        return participantDrinks(drink, participantId);
    }

    @PutMapping("{partyId}/participants/{participantId}/drinks/{drinkId}")
    @PartyMember
    public ParticipantDrinksDTO changeDrink(@PathVariable Integer partyId, @PathVariable Integer participantId,
            @PathVariable Integer drinkId,
            @RequestParam("volume") Float volume,
            @RequestParam("alcohol") Float alcoholPercentage){
        requireParticipant(partyId, participantId);
        Drink drink = drinkLog.correctAlcohol(participantId, drinkId,
                AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage));
        return participantDrinks(drink, participantId);
    }

    @DeleteMapping("{partyId}/participants/{participantId}/drinks/{drinkId}")
    @PartyMember
    public ParticipantDrinksDTO removeDrink(@PathVariable Integer partyId, @PathVariable Integer participantId,
            @PathVariable Integer drinkId){
        requireParticipant(partyId, participantId);
        drinkLog.undo(participantId, drinkId);
        return participantDrinks(null, participantId);
    }

    private ParticipantDrinksDTO participantDrinks(Drink drink, int participantId) {
        return ParticipantDrinksDTO.of(drink, promilles.of(userAccounts.get(participantId)));
    }

    private void requireParticipant(Integer partyId, Integer participantId) {
        if(!partyAccess.isMember(partyId, participantId)){
            throw new RuntimeException(MessageFormat.format("Participant {0} doesn''t belong to party {1}", participantId, partyId));
        }
    }

    @GetMapping("{partyId}/invitations")
    @PartyMember
    public List<Friend> suggestInvite(@LoggedInUserId int userId, @PathVariable Integer partyId, @RequestParam(defaultValue = "10", value="amount") int amount){
        return invitationSuggestions.forParty(userId, partyId, amount);
    }

    @PostMapping("{partyId}/invitations")
    @PartyMember
    public void invitePerson(@PathVariable Integer partyId, @RequestParam(value="userId") int userId){
        partyRoster.join(partyId, userId);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<String> handleInvalidTimestamp(DateTimeParseException ex) {
        return ResponseEntity.badRequest().body("Invalid timestamp: " + ex.getMessage());
    }
}
