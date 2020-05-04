package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType
import br.com.bluesoft.bee.util.StringUtil

class UserType implements Validator {

    def name
    def text = ''

    List validateWithMetadata(metadataView) {
        if (!(metadataView instanceof UserType)) {
            return []
        }

        def messages = []
        if (!StringUtil.compare(metadataView.text, this.text)) {
            def message = new Message(objectName: name, level: MessageLevel.ERROR, objectType: ObjectType.USER_TYPE, messageType: MessageType.USER_TYPE_BODY, message: "The body of the user type ${this.name} differs from metadata.")
            messages << message
        }

        return messages
    }
}
