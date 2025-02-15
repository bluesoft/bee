/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Bluesoft Consultoria em Informatica Ltda.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */
package br.com.bluesoft.bee.model

import br.com.bluesoft.bee.model.message.Message
import br.com.bluesoft.bee.model.message.MessageLevel
import br.com.bluesoft.bee.model.message.MessageType
import br.com.bluesoft.bee.util.RDBMS
import br.com.bluesoft.bee.util.StringUtil

class View implements Validator, WithDependencies {

    String name
    def text
    def text_oracle
    def text_postgres
    def text_mysql
    def text_redshift
    List<String> dependencies = []

    List validateWithMetadata(metadataView) {
        if (!(metadataView instanceof View)) {
            return []
        }

        def messages = []
        if (!StringUtil.compare(metadataView.text, this.text)) {
            def message = new Message(objectName: name, level: MessageLevel.ERROR, objectType: ObjectType.VIEW, messageType: MessageType.VIEW_BODY, message: "The body of the view ${this.name} differs from metadata.")
            messages << message
        }

        return messages
    }

    View getCanonical(RDBMS rdbms) {
        if(rdbms == null) {
            return this
        }

        String text
        switch(rdbms) {
            case RDBMS.ORACLE:
                text = text_oracle ?: this.text
                break
            case RDBMS.POSTGRES:
                text = text_postgres ?: this.text
                break
            case RDBMS.MYSQL:
                text = text_mysql ?: this.text
                break
            case RDBMS.REDSHIFT:
                text = text_redshift ?: this.text
                break
        }

        new View(name: name, text: text)
    }
}
