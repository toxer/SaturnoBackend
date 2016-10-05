package saturno.controller.auth
import grails.plugin.springsecurity.annotation.Secured


class ApisController {
	
	@Secured(['ROLE_ADMIN'])
	def test(){
		log.info("Test")
		render "test"
	}
	
	//senza regole
	def test2(){
		log.info("Test")
		render "test2"
	}
}
