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
package br.com.bluesoft.bee.schema

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.database.reader.DatabaseReaderChanger
import br.com.bluesoft.bee.exporter.JsonExporter
import br.com.bluesoft.bee.importer.JsonImporter
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.service.RulesConverter
import br.com.bluesoft.bee.util.RDBMSUtil
import groovy.sql.Sql

class BeeSchemaGeneratorAction implements ActionRunner {

    Options options
    BeeWriter out

    def sql
    def importer

    public boolean validateParameters() {
        return options.arguments.size() >= 1
    }

    public boolean run() {

        def clientName = options.arguments[0]
        def objectName = options.arguments[1]

        try {
            out.log "Connecting to the database..."
            sql = getDatabaseConnection(clientName)
        } catch (e) {
            throw new Exception("It was not possible to connect to the database.", e)
        }

        try {
            out.log "Extracting the metadata..."
            def databaseReader = DatabaseReaderChanger.getDatabaseReader(options, sql)
            Schema schemaNew = databaseReader.getSchema(objectName)
			if (objectName) {
				schemaNew = schemaNew.filter(objectName)
			}

            Schema schemaOld = getImporter().importMetaData()
			if (objectName) {
				schemaOld = schemaOld.filter(objectName)
			}

            schemaNew.rules = schemaOld.rules
            def converter = new RulesConverter()
            schemaNew.rdbms = RDBMSUtil.getRDBMS(options)
            schemaNew = converter.fromSchema(schemaNew)

            applyIgnore(schemaOld, schemaNew)
            applyList(schemaOld, schemaNew, 'views')
            applyList(schemaOld, schemaNew, 'mviews')
            applyList(schemaOld, schemaNew, 'procedures')
            applyList(schemaOld, schemaNew, 'triggers')

            def exporter = new JsonExporter(schemaNew, options.dataDir.canonicalPath)
            exporter.export();
            return true
        } catch (e) {
            e.printStackTrace()
            throw new Exception("Error importing database metadata.", e)
        }
    }

    void applyIgnore(Schema schemaOld, Schema schemaNew) {
        def tableNames = schemaOld.tables.findAll { it.key in schemaNew.tables }

        tableNames.each { etable ->
            def ignoredColumns = schemaOld.tables[etable.key].columns.findAll { it.value.ignore }
            ignoredColumns.each {
                if (schemaNew.tables[etable.key].columns[it.key]) {
                    schemaNew.tables[etable.key].columns[it.key].ignore = true
                }
            }
        }
    }

    void applyList(Schema schemaOld, Schema schemaNew, String field) {
        def items = schemaOld[field].findAll { it.key in schemaNew[field] }
        items.each {
            def item = it.value
            item.text_oracle = schemaNew[field][it.key].text_oracle ?: item.text_oracle
            item.text_postgres = schemaNew[field][it.key].text_postgres ?: item.text_postgres
            item.text_mysql = schemaNew[field][it.key].text_mysql ?: item.text_mysql
            item.text_redshift = schemaNew[field][it.key].text_redshift ?: item.text_redshift
            schemaNew[field][it.key] = item
        }
    }

    Sql getDatabaseConnection(clientName) {
        if (sql != null) {
            return sql
        }
        return ConnectionInfo.createDatabaseConnection(options.configFile, clientName)
    }

    private def getImporter() {
		if (importer == null) {
			return new JsonImporter(options.dataDir.canonicalPath)
		}
        return importer
    }
}
