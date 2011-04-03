package drinkcounter.authentication;

import java.util.List;
import java.util.Map;

import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.sreg.SRegRequest;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consolidates business logic from the UI code for Registration activities.
 * 
 * Most of this code modeled after ConsumerServlet, part of the openid4java 
 * sample code available at 
 * http://code.google.com/p/openid4java/wiki/SampleConsumer.
 * Some of this code was outright copied :->.
 * 
 * @author J Steven Perry
 * @author http://makotoconsulting.com
 *
 */
@Component
public class AuthenticationService {
	private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private ConsumerManager consumerManager;

    public AuthenticationService() {
        try {
            consumerManager = new ConsumerManager();
            consumerManager.setAssociations(new InMemoryConsumerAssociationStore());
            consumerManager.setNonceVerifier(new InMemoryNonceVerifier(10000));
		} catch (ConsumerException e) {
			String message = "Exception creating ConsumerManager!";
			log.error(message, e);
			throw new RuntimeException(message, e);
		}
    }



	
	/**
	 * Perform discovery on the User-Supplied identifier and return the
	 * DiscoveryInformation object that results from Association with the
	 * OP. This will probably be needed by the caller (stored in Session
	 * perhaps?).
	 * 
	 * I'm not thrilled about ConsumerManager being static, but it is
	 * very important to openid4java that the ConsumerManager object be the
	 * same instance all through a conversation (discovery, auth request, 
	 * auth response) with the OP. I didn't dig terribly deeply, but suspect
	 * that part of the key exchange or the nonce uses the ConsumerManager's
	 * hash, or some other instance-specific construct to do its thing.
	 * 
	 * @param userSuppliedIdentifier The User-Supplied identifier. It may already
	 *  be normalized.
	 *
	 *  @return DiscoveryInformation - The resulting DisoveryInformation object
	 *  returned by openid4java following successful association with the OP.
	 */
	@SuppressWarnings("unchecked")
	public DiscoveryInformation performDiscoveryOnUserSuppliedIdentifier(String userSuppliedIdentifier) {
		DiscoveryInformation ret = null;
		try {
		// Perform discover on the User-Supplied Identifier
		List<DiscoveryInformation> discoveries = consumerManager.discover(userSuppliedIdentifier);
		// Pass the discoveries to the associate() method...
		ret = consumerManager.associate(discoveries);

		} catch (DiscoveryException e) {
			String message = "Error occurred during discovery!";
			log.error(message, e);
			throw new RuntimeException(message, e);
		}
		return ret;
	}
	/**
	 * Create an OpenID Auth Request, using the DiscoveryInformation object
	 * return by the openid4java library.
	 * 
	 * This method also uses the Simple Registration Extension to grant
	 * the Relying Party (RP).
	 * 
	 * @param discoveryInformation The DiscoveryInformation that should have
	 *  been previously obtained from a call to 
	 *  performDiscoveryOnUserSuppliedIdentifier().
	 *  
	 *  @param returnToUrl The URL to which the OP will redirect once the
	 *   authentication call is complete.
	 *  
	 * @return AuthRequest - A "good-to-go" AuthRequest object packed with all
	 *  kinds of great OpenID goodies for the OpenID Provider (OP). The caller
	 *  must take this object and forward it on to the OP. Or call
	 *  processAuthRequest() - part of this Service Class.
	 */
	public AuthRequest createOpenIdAuthRequest(DiscoveryInformation discoveryInformation, String returnToUrl) {
		AuthRequest ret = null;
		//
		try {
			// Create the AuthRequest object
			ret = consumerManager.authenticate(discoveryInformation, returnToUrl);
			// Create the Simple Registration Request
			SRegRequest sRegRequest = SRegRequest.createFetchRequest();
			sRegRequest.addAttribute("email", false);
			sRegRequest.addAttribute("fullname", false);
			sRegRequest.addAttribute("dob", false);
			sRegRequest.addAttribute("postcode", false);
			ret.addExtension(sRegRequest);
			
		} catch (Exception e) {
			String message = "Exception occurred while building AuthRequest object!";
			log.error(message, e);
			throw new RuntimeException(message, e);
		}
		return ret;
	}
	
	/**
	 * Processes the returned information from an authentication request
	 * from the OP.
	 * 
	 * @param discoveryInformation DiscoveryInformation that was created earlier
	 *  in the conversation (by openid4java). This will need to be verified with
	 *  openid4java to make sure everything went smoothly and there are no
	 *  possible problems. This object was probably stored in session and retrieved
	 *  for use in calling this method.
	 *  
	 * @param pageParameters PageParameters passed to the page handling the
	 *  return verificaion.
	 *  
	 * @param returnToUrl The "return to" URL that was passed to the OP. It must
	 *  match exactly, or openid4java will issue a verification failed message
	 *  in the logs.
	 *  
	 * @return openId of the dude if authenticated, null otherwise
	 */
	public String processReturn(DiscoveryInformation discoveryInformation, Map<String, String> pageParameters, String returnToUrl) {
            String openId = null;
            // Verify the Information returned from the OP
            /// This is required according to the spec
            ParameterList response = new ParameterList(pageParameters);
            try {
                    VerificationResult verificationResult = consumerManager.verify(returnToUrl, response, discoveryInformation);
                    Identifier verifiedIdentifier = verificationResult.getVerifiedId();
                    AuthSuccess authSuccess = (AuthSuccess)verificationResult.getAuthResponse();
                    if (verifiedIdentifier != null && authSuccess != null) {
                            openId = verifiedIdentifier.getIdentifier();
                    }
            } catch (Exception e) {
                    String message = "Exception occurred while verifying response!";
                    log.error(message, e);
                    throw new RuntimeException(message, e);
            }
            
            return openId;
	}
}
