package drinkcounter.model;

import drinkcounter.alcoholcalculator.AlcoholCalculator;
import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 *
 * @author Toni
 */
@Entity
@NamedQueries({
    @NamedQuery(name="Drink.findByDrinker", query="SELECT d FROM Drink d WHERE d.drinker = ?1 ORDER BY d.timeStamp DESC")
})
public class Drink extends AbstractEntity {

    private User drinker;
    private Instant timeStamp;
    private float alcohol = (float)AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS;

    public Drink() {
        timeStamp = Instant.now();
    }

    @ManyToOne(fetch=FetchType.LAZY)
    public User getDrinker() {
        return drinker;
    }

    public void setDrinker(User drinkerKey) {
        this.drinker = drinkerKey;
    }

    // Keep the existing "timestamp without time zone" column instead of Hibernate's
    // default TIMESTAMP_UTC mapping for Instant, which would map to timestamptz.
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    public Instant getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Instant timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Amount of alcohol in drink in grams
     */
    public float getAlcohol() {
        return alcohol;
    }

    /**
     * Amount of alcohol in drink in grams
     */
    public void setAlcohol(float alcohol) {
        this.alcohol = alcohol;
    }

    @Transient
    public float getAmountOfShots() {
        return alcohol / (float)AlcoholCalculator.STANDARD_DRINK_ALCOHOL_GRAMS;
    }
}
