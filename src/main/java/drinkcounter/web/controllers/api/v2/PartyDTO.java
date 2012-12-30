/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.Party;
import java.util.Date;

/**
 *
 * @author Toni
 */
public class PartyDTO {
    private Integer id;
    private String name;
    private Date startTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return this.startTime;
    }
    
    public static PartyDTO fromParty(Party party){
        PartyDTO partyDTO = new PartyDTO();
        partyDTO.setId(party.getId());
        partyDTO.setName(party.getName());
        partyDTO.setStartTime(party.getStartTime());
        return partyDTO;
    }
}
