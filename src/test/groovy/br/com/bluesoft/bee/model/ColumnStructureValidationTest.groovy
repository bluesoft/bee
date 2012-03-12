package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType

class ColumnStructureValidationTest  extends spock.lang.Specification {

	def "it should return an error for each difference between the database and the reference metadata"() {

		given:
		def table = new Table(name:'people')

		Message message = databaseColumn.validateWithMetadata(table, metadataColumn)[0]
		message.messageType = messageType

		expect:
		message.message == messageText
		message.level == MessageLevel.ERROR
		message.objectType == ObjectType.TABLE_COLUMN
		message.objectName == databaseColumn.name

		where:
		databaseColumn     									| metadataColumn					| messageType 			   | messageText
		new TableColumn(name:'pessoa_key' ,type:'varchar')	| new TableColumn(type:'number')	| MessageType.DATA_TYPE    | 'The type of the column pessoa_key of the table people should be number but it is varchar'
		new TableColumn(name:'age' ,size:8)					| new TableColumn(size:5)			| MessageType.DATA_SIZE    | 'The size of the column age of the table people should be 5 but it is 8'
		new TableColumn(name:'value' ,scale:3)				| new TableColumn(scale:4)			| MessageType.DATA_SCALE   | 'The scale of the column value of the table people should be 4 but it is 3'
		new TableColumn(name:'name' ,nullable:true)			| new TableColumn(nullable:false)	| MessageType.NULLABILITY  | 'The nullable of the column name of the table people should be false but it is true'
		new TableColumn(name:'value' ,defaultValue:null)	| new TableColumn(defaultValue:'0')	| MessageType.DATA_DEFAULT | 'The defaultValue of the column value of the table people should be 0 but it is null'
	}

	def "it should return no message if there is no differente between the database and reference metadata"() {

		given:
		def table = new Table(name:'people')

		expect:
		databaseColumn.validateWithMetadata(table, metadataColumn)[0] == null

		where:
		databaseColumn     					| metadataColumn
		new TableColumn(type:'varchar')		| new TableColumn(type:'varchar')
		new TableColumn(size:8)				| new TableColumn(size:8)
		new TableColumn(scale:3)			| new TableColumn(scale:3)
		new TableColumn(nullable:true)		| new TableColumn(nullable:true)
		new TableColumn(defaultValue:'0')	| new TableColumn(defaultValue:'0')
	}
}
