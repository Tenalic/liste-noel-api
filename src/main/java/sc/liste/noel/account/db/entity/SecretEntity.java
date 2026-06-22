package sc.liste.noel.account.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "secret")
public class SecretEntity {
	
	@Id
	@Column(name = "nom_application")
	private String applicationName;

	@Column(name = "secret")
	private String secret;

	public SecretEntity() {
		super();
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
	
	

}
