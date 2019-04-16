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
package br.com.bluesoft.bee.data;

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.database.reader.*
import br.com.bluesoft.bee.importer.*
import br.com.bluesoft.bee.model.message.*
import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.service.MessagePrinter
import br.com.bluesoft.bee.util.CsvUtil

public class BeeDataValidatorAction implements ActionRunner {

    DatabaseReader databaseReader
    Importer importer

    BeeWriter out
    def options

    def sql
    def dataPath

    private def validateTableFromSource(def source, def dest, def level, def objectName, def message) {
        def messages = []

        def sourceSet = source as Set
        def destSet = dest as Set

        sourceSet.each {
            def row = it

            if (!destSet.contains(row)) {
                messages << new Message(messageType: MessageType.DATA_MISMATCH, level: level, objectName: objectName, message: message + "${row}")
            }
        }

        return messages
    }

    private def validateTable(def objectName, def schema, def sql) {
        out.log "validating ($objectName)..."
        def table = schema.tables[objectName]
        def databaseData = new TableDataReader(sql).getData(table)

        def file = new File(new File(options.dataDir, 'data'), objectName + ".csv")
        def fileData = CsvUtil.read(file)

        def messages = []

        if (fileData.size != databaseData.size) {
            messages << new Message(messageType: MessageType.DATA_MISMATCH, level: MessageLevel.ERROR, objectName: objectName, message: "There are ${fileData.size} (file) and ${databaseData.size} (database) lines in table $objectName")
        }

        messages.addAll(validateTableFromSource(databaseData, fileData, MessageLevel.WARNING, objectName, "This line was found in the database table [${objectName}] but was not found in the schema:"))
        messages.addAll(validateTableFromSource(fileData, databaseData, MessageLevel.ERROR, objectName, "This line was found in the schema [${objectName}] but was not found in the database:"))

        return messages
    }

    private def listFiles(def objectName, def schema) {
        def files = []
        def dataPath = new File(options.dataDir, 'data')

        if (objectName) {
            def file = new File(dataPath, objectName + ".csv")
			if (file.exists() && schema.tables[objectName]) {
				files = [objectName]
			}
        } else {
            def listFiles = dataPath.listFiles()
            listFiles.each {
                if (it.name.endsWith(".csv")) {
                    def tableName = it.name[0..-5]
					if (schema.tables[tableName]) {
						files << tableName
					}
                }
            }
        }

        return files
    }

    public boolean run() {
        def sql
        def path = options.dataDir.canonicalPath
        def clientName = options.arguments[0]
        def objectName = options.arguments[1]

        MessagePrinter messagePrinter = new MessagePrinter()

        out.log('importing schema metadata from the reference files')
        def schema = getImporter(path).importMetaData()

        try {
            out.log "Connecting to the database..."
            sql = getDatabaseConnection(clientName)
        } catch (e) {
            throw new Exception("It was not possible to connect to the database.", e)
        }

        def files = listFiles(objectName, schema)

        def messages = []

        files.each {
            messages.addAll validateTable(it, schema, sql)
        }

        def warnings = messages.findAll { it.level == MessageLevel.WARNING }
        def errors = messages.findAll { it.level == MessageLevel.ERROR }

        out.log("--- bee found ${warnings.size()} warning(s)")
        messagePrinter.print(out, warnings)
        out.log("--- bee found ${errors.size()} error(s)")
        messagePrinter.print(out, errors)

        return messages.size() == 0
    }

    @Override
    boolean validateParameters() {
        return true
    }

    def getDatabaseConnection(clientName) {
        if (sql != null) {
            return sql
        }
        return ConnectionInfo.createDatabaseConnection(options.configFile, clientName)
    }

    def getImporter(path) {
        if (importer == null) {
			return new JsonImporter(path)
		}
        return importer
    }
}
