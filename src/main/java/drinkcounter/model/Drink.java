/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;

/**
 *
 * @author Toni
 */
@Entity
@NamedQueries({
    @NamedQuery(name="Drink.findByDrinker", query="SELECT d FROM Drink d WHERE d.drinker = ?1 ORDER BY d.timeStamp DESC")
})
public class Drink extends AbstractEntity {

    private Participant drinker;
    private Date timeStamp;

    @ManyToOne
    public Participant getDrinker() {
        return drinker;
    }

    public void setDrinker(Participant drinkerKey) {
        this.drinker = drinkerKey;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
}
