/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

/**
 *
 * @author thardas
 */
public class HistoryPoint {
    private long timestamp;
    private double promilles;
    private boolean projected;
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public double getPromilles() {
        return promilles;
    }
    
    public void setPromilles(double promilles) {
        this.promilles = promilles;
    }

    /** True for points after now: the estimate of how promilles fall until the user is sober. */
    public boolean isProjected() {
        return projected;
    }

    public void setProjected(boolean projected) {
        this.projected = projected;
    }
}
