/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.Drink;

/**
 *
 * @author Toni
 */
public class DrinkDTO {
    private Integer id;
    private String timestamp;
    private Float amountOfShots;

    public static DrinkDTO fromDrink(Drink drink) {
        DrinkDTO dto = new DrinkDTO();
        dto.setId(drink.getId());
        dto.setTimestamp(drink.getTimeStamp().toString());
        dto.setAmountOfShots(drink.getAmountOfShots());
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public Float getAmountOfShots() {
        return amountOfShots;
    }
    
    public void setAmountOfShots(Float amountOfShots) {
        this.amountOfShots = amountOfShots;
    }
}
