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
package br.com.bluesoft.bee.data

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.database.reader.TableDataReader
import br.com.bluesoft.bee.importer.JsonImporter
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.runner.ActionRunnerMultipleParameter
import br.com.bluesoft.bee.util.CsvUtil

public class BeeDataGeneratorAction extends ActionRunnerMultipleParameter {

    def sql
    private Schema schema

    boolean execute(params) {
        def clientName = params[0]
        def objectName = params[1]

        def path = options.dataDir.canonicalPath

        def sql
        try {
            out.log "Connecting to the database..."
            sql = getDatabaseConnection(clientName)
        } catch (e) {
            throw new Exception("It was not possible to connect to the database.", e)
        }

        try {
            out.log "Extracting the table data to ${objectName} ... "
            def schema = getSchema(path)
            def table = schema.tables[objectName]
            def data = new TableDataReader(sql).getData(table)

            def dir = new File(path, "data")
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs()
            }

            def filename = objectName.toLowerCase() + ".csv"

            def file = new File(dir, filename)
            file.delete()

            CsvUtil.write file, data
            return true
        } catch (e) {
            out.log e.toString()
            throw new Exception("Error importing database metadata.", e)
        }
    }

    private Schema getSchema(path) {
        if (schema == null) {
            schema = new JsonImporter(path).importMetaData()
        }
        schema
    }

    @Override
    boolean validateParameters() {
        return options.arguments.size() >= 2
    }

    def getDatabaseConnection(clientName) {
        if (sql != null) {
            return sql
        }
        return ConnectionInfo.createDatabaseConnection(options.configFile, clientName)
    }
}
