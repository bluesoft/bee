package br.com.bluesoft.bee.model.postgres

import br.com.bluesoft.bee.model.ObjectType
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType

class PostgresPolicy {

    String name
    String cmd
    String usingExpression
    String checkExpression
    String[] roles
    boolean permissive

    def validateWithMetadata(Table table, PostgresPolicy metadataPolicy) {
        def messages = []

        if (metadataPolicy.cmd != this.cmd) {
            def messageText = "The cmd of the policy ${this.name} of the table ${table.name} should be ${metadataPolicy.cmd} but it is ${this.cmd}"
            messages << new Message(objectName: this.name, level: MessageLevel.ERROR, objectType: ObjectType.POLICY, messageType: MessageType.DATA_MISMATCH, message: messageText)
        }

        if (metadataPolicy.usingExpression != this.usingExpression) {
            def messageText = "The using expression of the policy ${this.name} of the table ${table.name} should be ${metadataPolicy.usingExpression} but it is ${this.usingExpression}"
            messages << new Message(objectName: this.name, level: MessageLevel.ERROR, objectType: ObjectType.POLICY, messageType: MessageType.DATA_MISMATCH, message: messageText)
        }

        if (metadataPolicy.checkExpression != this.checkExpression) {
            def messageText = "The check expression of the policy ${this.name} of the table ${table.name} should be ${metadataPolicy.checkExpression} but it is ${this.checkExpression}"
            messages << new Message(objectName: this.name, level: MessageLevel.ERROR, objectType: ObjectType.POLICY, messageType: MessageType.DATA_MISMATCH, message: messageText)
        }

        if (metadataPolicy.permissive != this.permissive) {
            def messageText = "The policy ${this.name} of the table ${table.name} should ${metadataPolicy.permissive ? 'be' : 'not be'} permissive"
            messages << new Message(objectName: this.name, level: MessageLevel.ERROR, objectType: ObjectType.POLICY, messageType: MessageType.DATA_MISMATCH, message: messageText)
        }

        messages.addAll validatePresenceOfRoles(table, metadataPolicy)

        return messages
    }

    private def validatePresenceOfRoles(Table table, PostgresPolicy metadataPolicy) {
        def messages = []

        def missingRoles = metadataPolicy.roles - this.roles
        def aditionalRoles = this.roles - metadataPolicy.roles

        missingRoles.each {
            def messageText = "The policy ${name} of the table ${table.name} is missing the role ${it}.";
            def message = new Message(objectName: "${table.name}.${name}", level: MessageLevel.ERROR, objectType: ObjectType.POLICY, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }

        aditionalRoles.each {
            def messageText = "The policy ${name} of the table ${table.name} has the additional role ${it}.";
            def message = new Message(objectName: "${table.name}.${name}", level: MessageLevel.ERROR, objectType: ObjectType.POLICY, messageType: MessageType.PRESENCE, message: messageText)
            messages << message
        }

        return messages
    }

    // forcing getter to avoid the creation of conflicting getters (is + get)
    boolean isPermissive() {
        return permissive
    }
}
