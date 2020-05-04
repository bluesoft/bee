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
package br.com.bluesoft.bee.dbchange

class SQLFileParser {

    static final String DELIMITER = ";"

    def parseFile(def file) {
        def statementsUp = null
        def statementsDown = null
        def statements = null
        def header = null
        def statement = ""
        def blockCode = false

        def lines = []
        file.eachLine { lines << it }

        lines.each {

            if (it.trim().toLowerCase() == "--!code") {
                blockCode = true
                return
            }

            if (it.trim() == "") {
                return
            }

            if (it.trim().startsWith("--")) {
                if (statements == null && header == null) {
                    header = (it - "--").trim()
                }
                return
            }

            if (it.trim() == "::up") {
                statementsUp = []
                statements = statementsUp
                return
            }

            if (it.trim() == "::down") {
                statementsDown = []
                statements = statementsDown
                return
            }

            if (!blockCode && it.trim().endsWith(DELIMITER)) {
                def pos = it.lastIndexOf(DELIMITER)
                statement += it[0..pos - 1] + "\n"

                if (statements == null) {
                    throw new IllegalStateException("!!!Erro: Wrong sql file format")
                }

                statements << statement
                statement = ""
                return
            }

            if (blockCode && it.trim() == "/") {
                if (statements == null) {
                    throw new IllegalStateException("!!!Erro: Wrong sql file format")
                }

                statements << statement
                statement = ""
                blockCode = false
                return
            }

            statement += it + "\n"
        }

        return [header: header, up: statementsUp, down: statementsDown]
    }
}
