package saturno.springauth

class AutenticationToken implements Serializable{
	private static final long serialVersionUID = 1
	String tokenValue
	String username
	String ente
	String sessionObject

	static mapping = { version false }
	
	static constraints={
		tokenValue nullable:false
		username nullable:false
		ente nullable:false
		sessionObject nullable:true
		
	}
}
