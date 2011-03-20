/*
1439 (@murgo) mikähän meillä ois yhen juojan nimi
1439 (@murgo) kun ei tuo nykyinen participant enää oikein kuvasta sitä
1440 (@murgo) jos ne eriytetään
1440 (@murgo) tai siis kun ne eriytetään
1440 (@murgo) oisko se sit Drinker vai User vai Person vai
1440 (@ville_salonen) Drinker
1440 (@ville_salonen) User ja Person on tylsemmille projekteille.
1441 (@ville_salonen) Tietty mitäs sit, jos halutaan ottaa kovemmat huumeet mukaan, niin heroiininpiikittäjä
                      ei oo enää Drinker!
1441 (@murgo) User kävis tähänkin :)
1442 (@ville_salonen) No, ihan miten vaan :)
1442 (@murgo) hehe, kait tajusit, hehe
1442 (@ville_salonen) Ah :D
1442 (@murgo) http://en.wikipedia.org/wiki/User
 */

package drinkcounter.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 *
 * @author Toni, Lauri
 */
@Entity
public class User extends AbstractEntity{

    public enum Sex {
        MALE(0.75f), FEMALE(0.66f);
        public float factor;
        Sex(float factor){
            this.factor = factor;
        }
    }

    private List<Party> parties;
    private String name;
    private float weight = 70;
    private Sex sex = Sex.MALE;
    private List<Drink> drinks = new ArrayList<Drink>();
    private AlcoholCalculator alcoholCalculator = new AlcoholCalculator(weight);

    /**
     * Guest of what? What is this I don't even
     */
    private boolean guest;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Transient
    public String getId(){
        return getStoreKey().toString();
    }

    @ManyToMany(fetch=FetchType.LAZY)
    public List<Party> getParties() {
        return parties;
    }

    public void setParties(List<Party> parties) {
        this.parties = parties;
    }

    public void drink(){
        alcoholCalculator.calculateDrink(new Date());
    }

    @Transient
    public float getPromilles(){
        return alcoholCalculator.getAlcoholAmountAt(new Date()) / (sex.factor * weight);
    }

    public List<Float> getPromillesAtInterval(Date start, Date end, int intervalMs) {
        List<Float> list = new ArrayList<Float>();
        for (long i = start.getTime(); i  <= end.getTime(); i += intervalMs) {
            list.add(alcoholCalculator.getAlcoholAmountAt(new Date(i)) / (sex.factor * weight));
        }

        return list;
    }

    @Transient
    public float getBloodAlcoholGrams() {
        return alcoholCalculator.getAlcoholAmountAt(new Date());
    }

    /**
     * Weight in kilos
     * @return
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Weight in kilos
     * @param weightInKilos
     */
    public void setWeight(float weightInKilos) {
        this.weight = weightInKilos;
        alcoholCalculator.setWeight(this.weight);
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Sex getSex() {
        return sex;
    }

    /**
     * What is this i don't even.
     * As far as I understand, this doesn't return total drinks like the name implies
     * but how many drinks has the user drunk since he was last sober
     * @return
     */
    @Transient
    public Integer getTotalDrinks(){
        // TODO optimize
        if (getDrinks().isEmpty()) return 0;
        Date soberTime = getTimeWhenUserLastSober();
        return drinksSince(soberTime);
    }

    private int drinksSince(Date date){
        int count = 0;
        // iterate from last to first
        ListIterator<Drink> iterator = drinks.listIterator(drinks.size());
        while(iterator.hasPrevious()){
            Drink drink = iterator.previous();
            if (drink.getTimeStamp().before(date))
                break;
            count++;
        }
        return count;
    }

    /**
     * When was the user sober last time? Accuracy is 15 minutes :P
     * @return Time when user was last sober
     */
    @Transient
    private Date getTimeWhenUserLastSober(){
        final int maxMinutes = 10080;
        final int interval = 15;

        long time = new Date().getTime();

        for (int i= 0; i < maxMinutes; i += interval) {
            float a = alcoholCalculator.getAlcoholAmountAt(new Date(time));
            if (a < 0.01) {
                break;
            }
            time -= interval * 60 * 1000;
        }
        return new Date(time);
    }

    public boolean isGuest() {
        return guest;
    }

    public void setGuest(boolean guest) {
        this.guest = guest;
    }

    /**
     * @return the drinks
     */
    @OneToMany
    public List<Drink> getDrinks() {
        return drinks;
    }

    /**
     * MUST BE SORTED BY TIME DESCENDING
     * Or must it? Shouldn't this be sorted time ascending?
     * @param drinks the drinks to set
     */
    public void setDrinks(List<Drink> drinks) {
        this.drinks = drinks;
        alcoholCalculator.reset();
        for (Drink drink : drinks) {
            if (drink.getTimeStamp() == null) continue;
            alcoholCalculator.calculateDrink(drink.getTimeStamp());
        }
    }
}