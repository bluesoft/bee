package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType;

class SchemaPresenceValidationTest extends spock.lang.Specification {

    def "tables missing in the database"() {

        given: "a database schema"
        Schema databaseSchema = new Schema()
        databaseSchema.tables = ['pessoa': new Table(name: 'pessoa')]

        and: "a metadata schema"
        Schema metadataSchema = new Schema()
        metadataSchema.tables = ['pessoa': new Table(name: 'pessoa'), 'produto': new Table(name: 'produto'), 'pedido': new Table(name: 'pedido')]

        when: "the schema is validated"
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then: "it should return a missing table message for each missing table"
        messages.size() == 2
        messages.find { it.objectName == 'produto' }.message == 'The database is missing the table produto.'
        messages.find { it.objectName == 'pedido' }.message == 'The database is missing the table pedido.'

        and: "the messages should have error level"
        messages.findAll { it.level == MessageLevel.ERROR }.size() == 2

        and: "the error type should be presence"
        messages.findAll { it.messageType == MessageType.PRESENCE }.size() == 2

        and: "the object type should be table"
        messages.findAll { it.objectType == ObjectType.TABLE }.size() == 2
    }

    def "additinal tables in the database"() {

        given: "a database schema"
        Schema databaseSchema = new Schema()
        databaseSchema.tables = ['pessoa': new Table(name: 'pessoa'), 'produto': new Table(name: 'produto'), 'pedido': new Table(name: 'pedido')]

        and: "a metadata schema"
        Schema metadataSchema = new Schema()
        metadataSchema.tables = ['pessoa': new Table(name: 'pessoa')]

        when: "the schema is validated"
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then: "it should return an additional table message for each additional table"
        messages.size() == 2
        messages.find { it.objectName == 'produto' }.message == 'The table produto exists in the database but does not exist in the reference metadata.'
        messages.find { it.objectName == 'pedido' }.message == 'The table pedido exists in the database but does not exist in the reference metadata.'

        and: "the messages should have warning level"
        messages.findAll { it.level == MessageLevel.WARNING }.size() == 2

        and: "the error type should be presence"
        messages.findAll { it.messageType == MessageType.PRESENCE }.size() == 2

        and: "the object type should be table"
        messages.findAll { it.objectType == ObjectType.TABLE }.size() == 2
    }

    def "missing and aditional views"() {

        given: "a database schema with two views"
        Schema databaseSchema = new Schema()
        databaseSchema.views = [view_menu: new View(name: 'view_menu', text: ""), view_carneiro: new View(name: 'view_carneiro', text: "")]

        and: "a metadata schema with two views"
        Schema metadataSchema = new Schema()
        metadataSchema.views = [view_menu: new View(name: 'view_menu', text: ""), view_cavalo: new View(name: 'view_cavalo')]

        when: "the schema is validated"
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then: "it should return a missing view message for the view view_cavalo"
        def missingMessage = messages.find { it.objectName == 'view_cavalo' }
        missingMessage.message == 'The database is missing the view view_cavalo.'

        and: "the messages should have error level"
        missingMessage.level == MessageLevel.ERROR

        and: "the error type should be presence"
        missingMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        missingMessage.objectType == ObjectType.VIEW

        then: "it should return an aditional view message for the view view_carneiro"
        def aditionalMessage = messages.find { it.objectName == 'view_carneiro' }
        aditionalMessage.message == 'The view view_carneiro exists in the database but does not exist in the reference metadata.'

        and: "the messages should have warning level"
        aditionalMessage.level == MessageLevel.WARNING

        and: "the error type should be presence"
        aditionalMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        aditionalMessage.objectType == ObjectType.VIEW
    }

    def "view and table with same name"() {

        given: "a database schema with one view called saldo_contabil_atual"
        Schema databaseSchema = new Schema()
        databaseSchema.views = [saldo_contabil_atual: new View(name: 'saldo_contabil_atual', text: "")]

        and: "a metadata schema with one table also called saldo_contabil_atual"
        Schema metadataSchema = new Schema()
        metadataSchema.tables = [saldo_contabil_atual: new Table(name: 'saldo_contabil_atual')]

        when: "the schema is validated"
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then: "it should return a missing table message for the table saldo_contabil_atual"
        def missingMessage = messages.find { it.objectName == 'saldo_contabil_atual' }
        missingMessage.message == 'The database contain one View with name saldo_contabil_atual, but reference metadata too contain a Table with same name.'

        and: "the messages should have error level"
        missingMessage.level == MessageLevel.ERROR

        and: "the error type should be presence"
        missingMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        missingMessage.objectType == ObjectType.TABLE
    }

    def "missing and aditional sequences"() {

        given: "a database schema with two sequences"
        Schema databaseSchema = new Schema()
        databaseSchema.sequences = [seq_menu: new Sequence(name: 'seq_menu'), seq_item: new Sequence(name: 'seq_item')]

        and: "a metadata schema with two sequences"
        Schema metadataSchema = new Schema()
        metadataSchema.sequences = [seq_menu: new Sequence(name: 'seq_menu'), seq_people: new Sequence(name: 'seq_people')]

        when: "the schema is validated"
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then: "it should return a missing sequence message for the sequence seq_people"
        def missingMessage = messages.find { it.objectName == 'seq_people' }
        missingMessage.message == 'The database is missing the sequence seq_people.'

        and: "the messages should have error level"
        missingMessage.level == MessageLevel.ERROR

        and: "the error type should be presence"
        missingMessage.messageType == MessageType.PRESENCE

        and: "the object type should be sequence"
        missingMessage.objectType == ObjectType.SEQUENCE

        then: "it should return an aditional sequence message for the sequence seq_item"
        def aditionalMessage = messages.find { it.objectName == 'seq_item' }
        aditionalMessage.message == 'The sequence seq_item exists in the database but does not exist in the reference metadata.'

        and: "the messages should have warning level"
        aditionalMessage.level == MessageLevel.WARNING

        and: "the error type should be presence"
        aditionalMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        aditionalMessage.objectType == ObjectType.SEQUENCE
    }

    def "missing and aditional procedures"() {

        given: "a database schema with two procedures"
        Schema databaseSchema = new Schema()
        databaseSchema.views = [proc1: new Procedure(name: 'proc1', text: ""), proc2: new Procedure(name: 'proc2', text: "")]

        and: "a metadata schema with two procedures"
        Schema metadataSchema = new Schema()
        metadataSchema.procedures = [proc1: new Procedure(name: 'proc1', text: ""), proc3: new Procedure(name: 'proc3')]

        when: "the schema is validated"
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then: "it should return a missing view message for the view view_cavalo"
        def missingMessage = messages.find { it.objectName == 'proc3' }
        missingMessage.message == 'The database is missing the procedure proc3.'

        and: "the messages should have error level"
        missingMessage.level == MessageLevel.ERROR

        and: "the error type should be presence"
        missingMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        missingMessage.objectType == ObjectType.PROCEDURE

        then: "it should return an aditional view message for the view view_carneiro"
        def aditionalMessage = messages.find { it.objectName == 'proc2' }
        aditionalMessage.message == 'The procedure proc2 exists in the database but does not exist in the reference metadata.'

        and: "the messages should have warning level"
        aditionalMessage.level == MessageLevel.WARNING

        and: "the error type should be presence"
        aditionalMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        aditionalMessage.objectType == ObjectType.PROCEDURE
    }

    def "missing and aditional packages"() {

        given: "a database schema with two procedures"
        Schema databaseSchema = new Schema()
        databaseSchema.packages = [pack1: new Package(name: 'pack1', text: "aaa", body: "bbb"), pack2: new Package(name: 'pack2', text: "bbb", body: "ccc")]

        and: "a metadata schema with two procedures"
        Schema metadataSchema = new Schema()
        metadataSchema.packages = [pack1: new Package(name: 'pack1', text: "aaa", body: "bbb"), pack3: new Package(name: 'pack3', text: "ccc", body: "ddd")]

        when: "the schema is validated"
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then: "it should return a missing package message for the view pack3"
        def missingMessage = messages.find { it.objectName == 'pack3' }
        missingMessage.message == 'The database is missing the package pack3.'

        and: "the messages should have error level"
        missingMessage.level == MessageLevel.ERROR

        and: "the error type should be presence"
        missingMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        missingMessage.objectType == ObjectType.PACKAGE

        then: "it should return an aditional view message for the view view_carneiro"
        def aditionalMessage = messages.find { it.objectName == 'pack2' }
        aditionalMessage.message == 'The package pack2 exists in the database but does not exist in the reference metadata.'

        and: "the messages should have warning level"
        aditionalMessage.level == MessageLevel.WARNING

        and: "the error type should be presence"
        aditionalMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        aditionalMessage.objectType == ObjectType.PACKAGE
    }

    def "missing and aditional triggers"() {

        given: "a database schema with two procedures"
        Schema databaseSchema = new Schema()
        databaseSchema.triggers = [trig1: new Trigger(name: 'trig', text: "aaa"), trig2: new Trigger(name: 'trig2', text: "bbb")]

        and: "a metadata schema with two procedures"
        Schema metadataSchema = new Schema()
        metadataSchema.triggers = [trig1: new Trigger(name: 'trig', text: "aaa"), trig3: new Trigger(name: 'trig3', text: "ccc")]

        when: "the schema is validated"
        def messages = databaseSchema.validateWithMetadata(metadataSchema)

        then: "it should return a missing package message for the view pack3"
        def missingMessage = messages.find { it.objectName == 'trig3' }
        missingMessage.message == 'The database is missing the trigger trig3.'

        and: "the messages should have error level"
        missingMessage.level == MessageLevel.ERROR

        and: "the error type should be presence"
        missingMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        missingMessage.objectType == ObjectType.TRIGGER

        then: "it should return an aditional view message for the view view_carneiro"
        def aditionalMessage = messages.find { it.objectName == 'trig2' }
        aditionalMessage.message == 'The trigger trig2 exists in the database but does not exist in the reference metadata.'

        and: "the messages should have warning level"
        aditionalMessage.level == MessageLevel.WARNING

        and: "the error type should be presence"
        aditionalMessage.messageType == MessageType.PRESENCE

        and: "the object type should be view"
        aditionalMessage.objectType == ObjectType.TRIGGER
    }
}
