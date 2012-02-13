/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

/**
 *
 * @author Toni
 */
public class DrinkDTO {
    private Integer id;
    private String timestamp;

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
}
