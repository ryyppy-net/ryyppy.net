package drinkcounter.web.controllers.ui;

import drinkcounter.authentication.RegistrationModel;
import drinkcounter.authentication.RegistrationService;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequest;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author murgo
 */
@Controller
public class AuthenticationController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);
    static DiscoveryInformation siirraTamaSessioon;

    @RequestMapping("/authenticate")
    public String parties(@RequestParam("openid") String openId) {
        log.info("Authenticating " + openId);
        try {
            DiscoveryInformation disco = RegistrationService.performDiscoveryOnUserSuppliedIdentifier(openId);

            // TODO save disco into session
            siirraTamaSessioon = disco;

            AuthRequest request = RegistrationService.createOpenIdAuthRequest(disco, RegistrationService.getReturnToUrl());

            return "redirect:" + request.getDestinationUrl(true);
        } catch (Exception ex) {
            log.error("Error authenticating OpenId", ex);
            return "redirect: vituiksmeniautentikointi";
        }
    }
    
    @RequestMapping("/openId")
    public String openId(ServletRequest request) {
        Map<String, String> pageParameters = new HashMap<String, String>();
        Map shitmap = request.getParameterMap();
        
        // shitty hack
        for (Object key : shitmap.keySet()) {
            pageParameters.put((String)key, ((String[])shitmap.get(key))[0]);
        }
        
        RegistrationModel registrationModel = new RegistrationModel();
        if (!pageParameters.isEmpty()) {
          //
          // If this is a return trip (the OP will redirect here once authentication
          /// is compelete), then verify the response. If it looks good, send the
          /// user to the RegistrationSuccessPage. Otherwise, display a message.
          //
          String isReturn = (String)pageParameters.get("is_return");
          if (isReturn != null && isReturn.equals("true")) {
            //
            // Grab the session object so we can let openid4java do verification.
            //
            // 
//            MakotoOpenIdAwareSession session = (MakotoOpenIdAwareSession)getSession();
//            DiscoveryInformation discoveryInformation = session.getDiscoveryInformation();
            //
            // Delegate to the Service object to do verification. It will return
            /// the RegistrationModel to use to display the information that was
            /// retrieved from the OP about the User-Supplied identifier. The
            /// RegistrationModel reference will be null if there was a problem
            /// (check the logs for more information if this happens).
            //
            registrationModel = RegistrationService.processReturn(siirraTamaSessioon, pageParameters, RegistrationService.getReturnToUrl());
            if (registrationModel == null) {
              //
              // Oops, something went wrong. Display a message on the screen.
              /// Check the logs for more information.
              //
              log.error("Open ID Confirmation Failed. No information was retrieved from the OpenID Provider. You will have to enter all information by hand into the text fields provided.");
              return "redirect:vituiksman";
            }
          }
        }

        // tämä on tuskin oikea tapa mutta
        if (registrationModel.getOpenId() == null || registrationModel.getOpenId().isEmpty())
              return "redirect:vituiksman";
        return "redirect:parties?openId=" + registrationModel.getOpenId();
    }

}
