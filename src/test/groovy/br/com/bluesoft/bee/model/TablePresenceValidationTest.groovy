package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.MessageLevel 
import br.com.bluesoft.bee.model.message.MessageType;

class TablePresenceValidationTest  extends spock.lang.Specification {
	
	def "a table with missing columns"() {
		
		given: "a table"
		Table databaseTable = new Table(name:'people')
		databaseTable.columns = [person_key:new TableColumn(name:'pessoa_key')]
		
		and: "a table metadata"
		Table tableMetadata = new Table(name:'people')
		tableMetadata.columns = [person_key:new TableColumn(name:'pessoa_key'), name:new TableColumn(name:'name')]
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(tableMetadata)
		
		then: "it should return a missing column message"
		messages.size() == 1
		def message = messages[0]
		message.message == 'The table people is missing the column name.'
		
		and: "the messages should have error level"
		message.level == MessageLevel.ERROR 
		
		and: "the error type should be presence"
		message.messageType == MessageType.PRESENCE 
		
		and: "the object type should be table"
		message.objectType == ObjectType.TABLE_COLUMN
	}
	
	def "a table with additional columns"() {
		
		given: "a table"
		Table databaseTable = new Table(name:'people')
		databaseTable.columns = [person_key:new TableColumn(name:'pessoa_key'), name:new TableColumn(name:'name')]
		
		and: "a table metadata"
		Table tableMetadata = new Table(name:'people')
		tableMetadata.columns = [person_key:new TableColumn(name:'pessoa_key')]
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(tableMetadata)
		
		then: "it should return an additional column message"
		messages.size() == 1
		def message = messages[0]
		message.message == 'The table people has the additional column name.'
		
		and: "the messages should have warning level"
		message.level == MessageLevel.WARNING
		
		and: "the error type should be presence"
		message.messageType == MessageType.PRESENCE
		
		and: "the object type should be table"
		message.objectType == ObjectType.TABLE_COLUMN
	}
	
	def "a table with a missing and an additional index"() {
		
		given: "a table"
		Table databaseTable = new Table(name:'people')
		databaseTable.indexes = [idx_pessoa_key:new Index(name:'idx_pessoa_key'), idx_pessoa_name:new Index(name:'idx_pessoa_name')]
		
		and: "a table metadata"
		Table tableMetadata = new Table(name:'people')
		tableMetadata.indexes = [idx_pessoa_key:new Index(name:'idx_pessoa_key'), idx_pessoa_data:new Index(name:'idx_pessoa_data')]
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(tableMetadata)
		
		then: "it should return an additional index message"
		def missingIndexMessage = messages.find { it.objectName == 'idx_pessoa_data' }
		missingIndexMessage.message == 'The table people is missing the index idx_pessoa_data.'
		
		and: "the messages should have error level"
		missingIndexMessage.level == MessageLevel.ERROR
		
		then: "it should return a missing index message"
		def additionalIndexMessage = messages.find { it.objectName == 'idx_pessoa_name' }
		additionalIndexMessage.message == 'The table people has the additional index idx_pessoa_name.'
		
		and: "the messages should have warning level"
		additionalIndexMessage.level == MessageLevel.WARNING
		
		and: "the error type should be presence in both messages"
		messages.findAll { it.messageType == MessageType.PRESENCE }.size() == 2
		
		and: "the object type should be table in both messages"
		messages.findAll { it.objectType == ObjectType.INDEX }.size() == 2
	}
	
	def "a table with a missing and an additional contraints"() {
		
		given: "a table"
		Table databaseTable = new Table(name:'people')
		databaseTable.constraints = [pk_pessoa:new Constraint(name:'idx_pessoa_key'), fk_tipo_pessoa:new Constraint(name:'fk_tipo_pessoa')]
		
		and: "a table metadata"
		Table tableMetadata = new Table(name:'people')
		tableMetadata.constraints = [pk_pessoa:new Constraint(name:'idx_pessoa_key'), uk_name:new Constraint(name:'uk_name')]
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(tableMetadata)
		
		then: "it should return an additional constraints message"
		def missingConstraintMessage = messages.find { it.objectName == 'uk_name' }
		missingConstraintMessage.message == 'The table people is missing the constraint uk_name.'
		
		and: "the messages should have error level"
		missingConstraintMessage.level == MessageLevel.ERROR
		
		then: "it should return a missing constraint message"
		def additionalConstraintMessage = messages.find { it.objectName == 'fk_tipo_pessoa' }
		additionalConstraintMessage.message == 'The table people has the additional constraint fk_tipo_pessoa.'
		
		and: "the messages should have warning level"
		additionalConstraintMessage.level == MessageLevel.WARNING
		
		and: "the error type should be presence in both messages"
		messages.findAll { it.messageType == MessageType.PRESENCE }.size() == 2  
		
		and: "the object type should be table  in both messages"
		messages.findAll { it.objectType == ObjectType.CONSTRAINT }.size() == 2
	}
}
