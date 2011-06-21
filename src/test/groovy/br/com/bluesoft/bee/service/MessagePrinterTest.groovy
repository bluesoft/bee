package br.com.bluesoft.bee.service

import java.io.StringWriter

import spock.lang.Specification
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.Table

class MessagePrintingTest extends Specification {

	def "printint messages"() {

		given: "there are diffences between the database and the metadata Schemas"

		StringWriter sw = new StringWriter()
		BeeWriter writer = [log: { msg -> sw.println(msg) }] as BeeWriter

		Schema databaseSchema = new Schema(tables:['pessoa':new Table(name:'pessoa'), 'dogs':new Table(name:'dogs')])
		Schema metadataSchema = new Schema(tables:['pessoa':new Table(name:'pessoa'), 'produto':new Table(name:'produto'), 'pedido':new Table(name:'pedido')])

		when: "the schema is validated"
		def messages = databaseSchema.validateWithMetadata(metadataSchema)

		and:"the message printer is involked"
		MessagePrinter printer = new MessagePrinter()
		printer.print(writer, messages)
		def textMessages = sw.toString().split('\n')

		then: "it should print the messages it the writter"
		textMessages.size() == 3

		and: "it should print the level of the message before the message text"
		textMessages[0].startsWith '[ERROR]'
		textMessages[1].startsWith '[ERROR]'
		textMessages[2].startsWith '[WARNING]'

		and: "it should print the message text"
		textMessages[0].endsWith 'The database is missing the table produto.'
		textMessages[1].endsWith 'The database is missing the table pedido.'
		textMessages[2].endsWith 'The table dogs exists in the database but doesnt exists in the reference metadata.'
	}
}
