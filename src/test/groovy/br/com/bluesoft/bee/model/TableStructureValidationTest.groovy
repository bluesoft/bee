package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.MessageLevel 
import br.com.bluesoft.bee.model.message.MessageType;

class TableStructureValidationTest  extends spock.lang.Specification {
	
	def "a temporary table that is not temporary in the database"() {
		
		given: "a table"
		Table databaseTable = new Table(name:'people', temporary:false)
		
		and: "a table metadata"
		Table tableMetadata = new Table(name:'people', temporary:true)
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(tableMetadata)
		
		then: "it should return an error message because the table should be temporary"
		messages[0].message == 'The table people should be temporary.'
		
		and: "the messages should have error level"
		messages[0].level == MessageLevel.ERROR 
		
		and: "the error type should be presence"
		messages[0].messageType == MessageType.TEMPORARY
		
		and: "the object type should be table"
		messages[0].objectType == ObjectType.TABLE
	}
	
	def "a regular table that is temporary in the database"() {
		
		given: "a table"
		Table databaseTable = new Table(name:'people', temporary:true)
		
		and: "a table metadata"
		Table tableMetadata = new Table(name:'people', temporary:false)
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(tableMetadata)
		
		then: "it should return an error message because the table should not be temporary"
		messages[0].message == 'The table people should not be temporary.'
		
		and: "the messages should have error level"
		messages[0].level == MessageLevel.ERROR
		
		and: "the error type should be presence"
		messages[0].messageType == MessageType.TEMPORARY
		
		and: "the object type should be table"
		messages[0].objectType == ObjectType.TABLE
	}
	
	def "it should call the validateMetadata of each column of the metadata"() {
		
		given: "a table metadata"
		Table metadataTable = new Table(name:'people')
		metadataTable.columns = [pessoa_key: new TableColumn(name:'pessoa_key'), name: new TableColumn(name:'name')]
		
		and: "a table"
		Table databaseTable = new Table(name:'people')
		def databaseColumn1 = Mock(TableColumn)
		def databaseColumn2 = Mock(TableColumn)
		databaseTable.columns = [pessoa_key: databaseColumn1, name: databaseColumn2]
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(metadataTable)
		
		then: "it should call the validateMetadata of each column of the metadata"
		1 * databaseTable.columns['pessoa_key'].validateWithMetadata(!null, !null) >> []
		1 * databaseTable.columns['name'].validateWithMetadata(!null, !null) >> []
	}
	
	def "it should call the validateMetadata of each index of the metadata"() {
		
		given: "a table"
		Table metadataTable = new Table(name:'people')
		metadataTable.indexes = [idx_pessoa_key: new Index(name:'idx_pessoa_key'), idx_nome: new Index(name:'idx_nome')]
		
		and: "a table metadata"
		Table databaseTable = new Table(name:'people')
		def indexMock1 = Mock(Index)
		def indexMock2 = Mock(Index)
		databaseTable.indexes = [idx_pessoa_key: indexMock1, idx_nome: indexMock2]
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(metadataTable)
		
		then: "it should call the validateMetadata of each index of the metadata"
		1 * databaseTable.indexes['idx_pessoa_key'].validateWithMetadata(!null, !null) >> []
		1 * databaseTable.indexes['idx_nome'].validateWithMetadata(!null, !null) >> []
	}
	
	def "it should call the validateMetadata of each constraint of the metadata"() {
		
		given: "a table metadata"
		Table metadataTable = new Table(name:'people')
		metadataTable.constraints = [pk_pessoa: new Constraint(name:'pk_pessoa'), fk_tipo_pessoa: new Constraint(name:'fk_tipo_pessoa')]
		
		and: "a table"
		Table databaseTable = new Table(name:'people')
		def constraintMock1 = Mock(Constraint)
		def constraintMock2 = Mock(Constraint)
		databaseTable.constraints = [pk_pessoa: constraintMock1, fk_tipo_pessoa: constraintMock2]
		
		when: "the table is validated"
		def messages = databaseTable.validateWithMetadata(metadataTable)
		
		then: "it should call the validateMetadata of each column of the metadata"
		1 * databaseTable.constraints['pk_pessoa'].validateWithMetadata(!null, !null) >> []
		1 * databaseTable.constraints['fk_tipo_pessoa'].validateWithMetadata(!null, !null) >> []
	}
}
