package drinkcounter.authentication;

import java.io.Serializable;
import java.util.Date;

/**
 * This class serves as the model for registration information. It can be
 * used as the domain object for saving information to the DB (through, say,
 * Hibernate or another ORM tool), and the model for the Wicket forms used
 * by this application.
 * 
 * Note: if you're going to use Hibernate, generate equals() (but I'd leave
 * hashCode() alone).
 *  
 * @author J Steven Perry
 * @author http://makotoconsulting.com
 */
public class RegistrationModel implements Serializable {
  /**
	 * 
	 */
	private static final long serialVersionUID = -2834908356868001273L;

	private String openId;
	private String fullName;
	private String emailAddress;
	private String zipCode;
	private Date dateOfBirth;
	private String favoriteColor;
	
	public String getOpenId() {
		return openId;
	}
	public void setOpenId(String openId) {
		this.openId = openId;
	}
  public String getFullName() {
    return fullName;
  }
  public void setFullName(String fullName) {
    this.fullName = fullName;
  }
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	public String getZipCode() {
		return zipCode;
	}
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}
	public Date getDateOfBirth() {
		return dateOfBirth;
	}
	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}
	public String getFavoriteColor() {
		return favoriteColor;
	}
	public void setFavoriteColor(String favoriteColor) {
		this.favoriteColor = favoriteColor;
	}

}
