package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Toni
 */
@RestController
@RequestMapping("API/v2/parties")
public class PartyApiController {

    private final CurrentUser currentUser;
    private final DrinkCounterService drinkCounterService;
    private final UserService userService;

    public PartyApiController(CurrentUser currentUser, DrinkCounterService drinkCounterService, UserService userService) {
        this.currentUser = currentUser;
        this.drinkCounterService = drinkCounterService;
        this.userService = userService;
    }

    @GetMapping
    public List<PartyDTO> getParties(){
        List<Party> parties = currentUser.getUser().getParties();
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
    public PartyDTO addParty(@RequestParam("name") String partyName){
        User user = currentUser.getUser();
        Party party = drinkCounterService.startParty(partyName);
        drinkCounterService.linkUserToParty(user.getId(), party.getId());
        return PartyDTO.fromParty(party);
    }

    @GetMapping("{partyId}")
    public PartyDTO getParty(@PathVariable Integer partyId){
        Party party = drinkCounterService.getParty(partyId);
        return PartyDTO.fromParty(party);
    }

    @GetMapping("{partyId}/participants")
    public List<ParticipantDTO> getParticipants(@PathVariable Integer partyId){
        Party party = drinkCounterService.getParty(partyId);
        List<User> participants = party.getParticipants();
        List<ParticipantDTO> participantDTOs = new ArrayList<ParticipantDTO>();
        for (User participant : participants) {
            ParticipantDTO participantDTO = ParticipantDTO.fromUser(participant);
            
            List<HistoryPoint> history = SlopeService.getSlopes(participant);
            participantDTO.setHistory(history);
            
            participantDTOs.add(participantDTO);
        }
        return participantDTOs;
    }

    @PostMapping("{partyId}/participants")
    public void addParticipant(@PathVariable Integer partyId, @RequestParam(value="email", required=false) String email,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="sex", required=false) User.Sex sex,
            @RequestParam(value="weight", required=false) Float weight){
        if(email != null){
            User user = userService.getUserByEmail(email);
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
        userService.addUser(user);
        drinkCounterService.linkUserToParty(user.getId(), partyId);
    }

    @DeleteMapping("{partyId}/participants/{participantId}")
    public void removeParticipant(@PathVariable Integer partyId, @PathVariable Integer participantId){
        drinkCounterService.unlinkUserFromParty(participantId, partyId);
    }

    @GetMapping("{partyId}/participants/{participantId}")
    public ParticipantDTO getParticipant(@PathVariable Integer partyId, @PathVariable Integer participantId){
        Party party = drinkCounterService.getParty(partyId);
        User participant = userService.getUser(participantId);
        if(!party.getParticipants().contains(participant)){
            throw new RuntimeException(MessageFormat.format("Participant {} doesn't belong to party {}", participant.getId(), party.getId()));
        }
        return ParticipantDTO.fromUser(participant);
    }

    @PostMapping("{partyId}/participants/{participantId}/drinks")
    public void drink(@PathVariable Integer partyId, @PathVariable Integer participantId,
            @RequestParam(value="volume", required=false) Float volume,
            @RequestParam(value="alcohol", required=false) Float alcoholPercentage,
            @RequestParam(value="timestamp", required=false) String timestamp){
        Party party = drinkCounterService.getParty(partyId);
        User participant = userService.getUser(participantId);
        if(!party.getParticipants().contains(participant)){
            throw new RuntimeException(MessageFormat.format("Participant {} doesn't belong to party {}", participant.getId(), party.getId()));
        }
        float alcoholAmount = (float)AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS;
        if (volume != null && alcoholPercentage != null) {
            alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
        }
        Date time = null;
        if(timestamp != null){
            time = new Date(new DateTime(timestamp).getMillis());
        }
        drinkCounterService.addDrink(participantId, time, alcoholAmount);
    }

    @GetMapping("{partyId}/invitations")
    public List<Friend> suggestInvite(@PathVariable Integer partyId,  @RequestParam(defaultValue = "10", value="amount") int amount){
        return drinkCounterService.suggestInvitations(currentUser.getUser().getId(), partyId, amount);
    }

    @PostMapping("/parties/{partyId}/invitations")
    public void invitePerson(@PathVariable Integer partyId, @RequestParam(value="userId") int userId){
        drinkCounterService.linkUserToParty(userId, partyId);
    }
}
