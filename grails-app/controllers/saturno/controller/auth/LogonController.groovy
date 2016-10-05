package saturno.controller.auth

import grails.converters.JSON
import groovy.sql.Sql

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.security.access.annotation.Secured

import saturno.springauth.AutenticationToken
import saturno.springauth.Role
import saturno.springauth.User
import saturno.springauth.UserRole

class LogonController {
	
	JSONObject ERROR_CODES=JSON.parse("{'0':'LOGIN OK','1':'USER NOT FOUND','2':'ENTE NOT FOUND','5':'ENTE NOT VALID FOR USER','4':'AUTH METHOD ERROR'}")
	
	def dataSource_portal
	def rest
	def tokenStorageService
	def tokenRequest(){

		//risposta
		JSONObject resp = new JSONObject();

		//prelevo l'utente
		def json = request.JSON
		def username = json?.username
		if (username == null){
			resp.errorCode=1
			resp.desc=ERROR_CODES."${resp.errorCode}"
			render resp as JSON
			return;
		}
		//prelevo l'ente
		def ente = json?.ente
		if (ente==null){
			resp.errorCode=2
			resp.desc="Ente not found"
			render resp as JSON
			return;
		}

		//TODO creare un sistema di reperimento ruoli da request
		String roleFake = "ROLE_ADMIN"

		//controllo se per quell'utente e ente esiste già un token, nel caso lo restituisco

		AutenticationToken token = AutenticationToken.findByUsernameAndEnte(username+"_"+ente,ente);

		if (token != null){
			resp.errorCode=0
			resp.desc=ERROR_CODES."${resp.errorCode}"
			resp.token = token.getTokenValue()
			resp.sessionObj=token.sessionObject;
			render resp as JSON
			log.info("Restituiro il token "+token+" per l'utente "+username+" e l'ente "+ente)
			return
		}

		//se non ho un token valido, creo l'utente e il token

		//controllo che l'utente abbia l'accesso per quell'ente e quel ruolo

		//TODO inserire controllo sicurezza utente ente
		
		
		def parametri = [userId:username,appName:grailsApplication.config.grails.applicationauth,aziendaId:ente];
		
		Sql sql = Sql.newInstance(dataSource_portal);
		
		def rows= sql.rows("select count(*) as number from utenti_aziende_aplicazioni ap join aziende az on (ap.aziendaId=az.aziendaId)  where sigla=:appName and userId=:userId and ap.aziendaId=:aziendaId", parametri)
		if (rows.get(0).number<1){
			resp.errorCode=5
			resp.desc=ERROR_CODES."${resp.errorCode}"
			render resp as JSON
			return
		}
		
		
		
		
		sql.close();
		User user = User.findByUsername(username+"_"+ente)

		if (user == null){
			//salvo un utente con il nome e i ruolo
			//la user è data dalla concatenazione di username e ente
			//TODO inserire ruolo

			user = User.create()
			user.setUsername(username+"_"+ente)
			user.setPassword("1")
			user.setEnabled(true)
			user.setAccountExpired(false)
			user.setAccountLocked(false)
			user.save(flush: true, failOnError:true);
			log.info("Inserito in db l'user "+user.getUsername())
		}
		else{
			log.info ("Trovato l'utente "+user.getUsername())
		}

		//I ruoli possibili sono creati nel BootStrap

		Role role = Role.findByAuthority(roleFake)

		UserRole userRole = UserRole.findByUserAndRole(user,role)
		if (userRole == null){
			userRole = UserRole.create()
			userRole.setRole(role)
			userRole.setUser(user)
			userRole.save(flush: true, failOnError:true);
			log.info("Creato userRole per l'utente "+user.getUsername()+" e il ruolo"+role.getAuthority())
		}

		//effettuo un login che forza la creazione del token


		JSONObject loginJson = new JSONObject();
		loginJson.username=user.getUsername()
		loginJson.password="1"
		def url = grailsApplication.config.grails.loginUrl;
		def loginResp=rest.post(url){
			accept("application/json")
			contentType("application/json")
			body (loginJson?.toString())
		}

		log.info(loginResp.responseEntity?.body)

		if(loginResp.responseEntity!=null){
			//aggiungo il token
			AutenticationToken authToken = AutenticationToken.create();
			authToken.setUsername(user.getUsername())
			authToken.setTokenValue( JSON.parse(loginResp.responseEntity.body).access_token)
			authToken.setEnte(ente)
			JSONObject sessionObj = new JSONObject();
			sessionObj.ente=ente
			sessionObj.username=username
			sessionObj.sessionParameter = new JSONArray()
			authToken.setSessionObject(sessionObj.toString());
			authToken.save(flush: true, failOnError:true);
			resp.errorCode=0
			resp.desc=ERROR_CODES."${resp.errorCode}"
			resp.token = authToken.getTokenValue()
			resp.sessionObj=authToken.getSessionObject();
			render resp as JSON
			return
		}
		resp.errorCode=4
		resp.desc=ERROR_CODES."${resp.errorCode}"

		render resp as JSON

	}
	
	//metodo accessibile solo se loggato
	
	@Secured(['ROLE_ADMIN','ROLE_CONSULTATORE','ROLE_PIANIFICATORE'])	
	def logout(){
		def token = (request.getHeader("Authorization").replace("Bearer","")).trim()
		User user = User.findByUsername(tokenStorageService.loadUserByToken(token)?.username)
		if (user!=null){
			UserRole.findByUser(user)?.delete(flush: true, failOnError:true)
			AutenticationToken.findByTokenValue(token).delete(flush: true, failOnError:true)
			user.delete(flush: true, failOnError:true)
			render user as JSON
			return
		}
		render null;
		
	
	}
	
	
	
}
