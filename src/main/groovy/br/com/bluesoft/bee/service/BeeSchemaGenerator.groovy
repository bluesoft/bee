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
package br.com.bluesoft.bee.service

import br.com.bluesoft.bee.database.ConnectionInfo
import br.com.bluesoft.bee.database.reader.OracleDatabaseReader
import br.com.bluesoft.bee.exporter.JsonExporter
import br.com.bluesoft.bee.model.Schema


class BeeSchemaGenerator {

	BeeWriter out
	String objectName
	String path
	String configName
	String clientName

	def sql

	BeeSchemaGenerator(String objectName) {
		this.objectName = objectName
	}

	BeeSchemaGenerator() {
		this(null)
	}

	public void run(){
		def sql

		try {
			out.log "Connecting to the database..."
			sql = getDatabaseConnection()
		} catch (e){
			throw new Exception("It was not possible to connect to the database.",e)
		}

		try {
			out.log "Extracting the metadata..."
			def databaseReader = new OracleDatabaseReader(sql)
			def Schema schema = databaseReader.getSchema(objectName)
			if(objectName)
				schema = schema.filter(objectName)
			def exporter = new JsonExporter(schema, path)
			exporter.export();
		} catch(e) {
			e.printStackTrace()
			throw new Exception("Error importing database metadata.",e)
		}
	}

	def getDatabaseConnection() {
		if(sql != null) {
			return sql
		}
		return ConnectionInfo.createDatabaseConnection(configName, clientName)
	}
}
