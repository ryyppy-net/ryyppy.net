/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.web.controllers.internal;

import drinkcounter.DrinkCounterService;
import java.io.Writer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Toni
 */
@Controller
public class InternalActionsController {

    @Autowired
    private DrinkCounterService drinkCounterService;

    @RequestMapping("/burnAlcohol")
    public void burnAlcohol(@RequestParam("timePassed") float hours, Writer writer){
        drinkCounterService.timePassed(hours);
    }

}
