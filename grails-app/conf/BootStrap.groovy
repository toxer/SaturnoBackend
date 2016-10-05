import saturno.springauth.Role

class BootStrap {

	def init = { servletContext ->
		//		def adminRole = new Role('ROLE_ADMIN').save()
		//		def userRole = new Role('ROLE_USER').save()
		//
		//		def testUser = new User('me', 'password').save()
		//
		//		UserRole.create testUser, adminRole, true
		//
		//		assert User.count() == 1
		//		assert Role.count() == 2
		//		assert UserRole.count() == 1


		//creazione dei ruoli

		def ruoli = [
			'ROLE_ADMIN',
			'ROLE_CONSULTATORE',
			'ROLE_PIANIFICATORE'
		]

		ruoli.each { 
			if (Role.findByAuthority(it)==null){
				//aministratore
				new Role(it).save(flush: true, failOnError:true);
				log.info("Creato e inserito il nuovo ruolo "+it)
			}
			else{
				log.info("Trovato il ruolo "+it)
			} 
		}
	}
	def destroy = {
	}
}
