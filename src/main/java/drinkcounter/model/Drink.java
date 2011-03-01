/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import com.google.appengine.api.datastore.Key;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Temporal;

/**
 *
 * @author Toni
 */
@Entity
@NamedQueries({
    @NamedQuery(name="Drink.findByDrinker", query="SELECT d FROM Drink d WHERE d.drinkerKey = ?1 ORDER BY d.timeStamp DESC")
})
public class Drink extends AbstractEntity {

    private Key drinkerKey;
    private Date timeStamp;

    public Key getDrinkerKey() {
        return drinkerKey;
    }

    public void setDrinkerKey(Key drinkerKey) {
        this.drinkerKey = drinkerKey;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
}
