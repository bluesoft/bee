package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.MessageType;
import br.com.bluesoft.bee.model.message.Message;
import br.com.bluesoft.bee.model.message.MessageLevel;

class ConstraintsStructureValidationTest  extends spock.lang.Specification {
	
	def "it should return an error for each difference between the database and the reference metadata"() {
		
		given:
		def table = new Table(name:'people')
		
		expect:
		Message message = databaseConstraint.validateWithMetadata(table, metadatConstraint)[0] 
		message.message == messageText
		message.level == MessageLevel.ERROR
		message.objectType == ObjectType.CONSTRAINT
		message.messageType == messageType 
		message.objectName == databaseConstraint.name
		
		where:
		databaseConstraint     											| metadatConstraint							| messageType 			   					| messageText   																															
		new Constraint(name:'pk_person' ,type:'R')						| new Constraint(type:'P')					| MessageType.CONSTRAINT_TYPE    			| 'The type of the constraint pk_person of the table people should be P but it is R'
		new Constraint(name:'fk_dogs' ,refTable:'cats')					| new Constraint(refTable:'dogs')			| MessageType.CONSTRAINT_REF_TABLE  		| 'The referenced table of the constraint fk_dogs of the table people should be dogs but it is cats'
		new Constraint(name:'fk_dogs' ,onDelete:'cascade', type:'R')	| new Constraint(name:'fk_dogs',type:'R')	| MessageType.CONSTRAINT_DELETE_RULE  		| 'The delete action of the constraint fk_dogs should be null but it is cascade'
		new Constraint(name:'uk_person' ,columns:['id','name'])			| new Constraint(columns:['name','id'])		| MessageType.CONSTRAINT_COLUMNS   			| 'The columns of the constraint uk_person of the table people should be [name, id] but it is [id, name]'
		new Constraint(name:'fk_users' ,refColumns:['id','name'])		| new Constraint(refColumns:['name','id'])	| MessageType.CONSTRAINT_REF_COLUMNS		| 'The columns of the constraint fk_users of the table people should be [name, id] but it is [id, name]'
	}
	
	def "it should return no message if there is no differente between the database and reference metadata"() {
		
		given:
		def table = new Table(name:'people')
		
		expect:
		databaseConstraint.validateWithMetadata(table, metadataConstraint)[0] == null
		
		where:
		databaseConstraint     						| metadataConstraint					
		new Constraint(type:'N')					| new Constraint(type:'N')	
		new Constraint(refTable:'people')			| new Constraint(refTable:'people')			
		new Constraint(columns:['id','name'])		| new Constraint(columns:['id','name'])
	}
}
