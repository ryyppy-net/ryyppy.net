/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 *
 * @author Toni, Lauri
 */
@Entity
public class Participant extends AbstractEntity{

    public enum Sex {
        MALE(0.75f), FEMALE(0.66f);
        public float factor;
        Sex(float factor){
            this.factor = factor;
        }
    }
    private String name;
    private Party party;
    private float weight = 70;
    private Sex sex = Sex.MALE;
    private List<Drink> drinks = new ArrayList<Drink>();
    private AlcoholCalculator alcoholCalculator = new AlcoholCalculator(weight);
    
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

    @ManyToOne(fetch=FetchType.LAZY)
    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
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
        for (long i = start.getTime(); i  < end.getTime(); i += intervalMs) {
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
        alcoholCalculator.calculateBurnRate(this.weight);
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Sex getSex() {
        return sex;
    }

    @Transient
    public Integer getTotalDrinks(){
        return this.drinks.size();
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
