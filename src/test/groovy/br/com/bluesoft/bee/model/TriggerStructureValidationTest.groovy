package br.com.bluesoft.bee.model;

import br.com.bluesoft.bee.model.message.Message 
import br.com.bluesoft.bee.model.message.MessageLevel 
import spock.lang.Specification;

public class TriggerStructureValidationTest extends Specification {
	def "it should return an error for each difference between the database and the reference metadata"() {
		given: 'a metadata trigger'
		def metadataTrigger = new Trigger(name:'trig1', text: 'xyz')
		
		and: "a database package"
		def databaseTrigger = new Trigger(name:'trig1', text: 'aaa')
		
		when: "a package is validated"
		def messages = databaseTrigger.validateWithMetadata(metadataTrigger)
		
		then: "is shoud return error a error message because the trigger body are different"
		def Message message = messages[0]
		message.objectType == ObjectType.TRIGGER
		message.level == MessageLevel.ERROR
		message.message == 'The body of the trigger trig1 differs from metadata.'
	}
}
