package types;
/**
 * 
 * Specify resource = "any" for file list
 * 
 * @author I069922
 *
 */
public class DelegateRequest {

	private String subject;
	
	private String resource;
	//private String password;
	
	public DelegateRequest(){}
	
	public DelegateRequest(String subject, String resource){
		this.subject = subject;
		this.resource = resource;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getResource() {
		return resource;
	}
	/**
	 * 
	 * @param resource resource name, or "any" for 
	 * getting all available resources
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}
}