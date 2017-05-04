import java.io.Serializable;

public class TinyUser implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String user;
	String password;
	String token;
	public TinyUser(String user, String token2, String password) {
		this.user=user;
		this.token=token2;
		this.password=password;
	}
	public String getUser(){
		return this.user;
	}
	public String getToken(){
		return this.token;
	}
	public String getPassword(){
		return this.password;
	}
}
