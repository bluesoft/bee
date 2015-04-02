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
package br.com.bluesoft.bee.exporter;

import java.io.File

import org.codehaus.jackson.map.ObjectMapper

import br.com.bluesoft.bee.importer.JsonImporter
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.util.JsonUtil

public class JsonExporter implements Exporter {

	String path
	Schema schema
	ObjectMapper mapper

	File mainFolder
	File tablesFolder
	File sequencesFolder
	File viewsFolder
	File proceduresFolder
	File packagesFolder
	File triggersFolder
	File userTypesFolder

	JsonExporter(Schema schema) {
		this(schema, null)
	}

	JsonExporter(Schema schema, String path) {
		this.schema = schema
		this.path = path ?: '/tmp/bee'
		this.mapper = JsonUtil.createMapper()
	}

	void createPath() {
		mainFolder = new File(this.path)
		mainFolder.mkdirs()

		tablesFolder = new File(mainFolder, 'tables')
		tablesFolder.mkdir()
		
		sequencesFolder = new File(mainFolder, 'sequences')
		sequencesFolder.mkdir()

		viewsFolder = new File(mainFolder, 'views')
		viewsFolder.mkdir()

		proceduresFolder = new File(mainFolder, 'procedures')
		proceduresFolder.mkdir()

		packagesFolder = new File(mainFolder, 'packages')
		packagesFolder.mkdir()

		triggersFolder = new File(mainFolder, 'triggers')
		triggersFolder.mkdir()
		
		userTypesFolder = new File(mainFolder, 'usertypes')
		userTypesFolder.mkdir()

	}

	void export() {
		createPath()
		createTableFiles(schema.getTables())
		createSequenceFiles(schema.getSequences())
		createViewFiles(schema.getViews())
		createProceduresFiles(schema.getProcedures())
		createPackagesFiles(schema.getPackages())
		createTriggersFiles(schema.getTriggers())
		createUserTypesFiles(schema.getUserTypes())
	}

	void createTableFiles(def tables) {
		tables.each {
			mapper.writeValue(new File(tablesFolder, "${it.value.name}.bee"), it.value)
		}
	}

	void createViewFiles(def views) {
		views.each {
			mapper.writeValue(new File(viewsFolder, "${it.value.name}.bee"), it.value)
		}
	}

	void createSequenceFiles(def sequences) {
		sequences.each {
			mapper.writeValue(new File(sequencesFolder, "${it.value.name}.bee"), it.value)
		}
	}

	void createProceduresFiles(def procedures) {
		procedures.each { name, procedure ->
			mapper.writeValue(new File(proceduresFolder, "${name}.bee"), procedure)
		}
	}

	void createPackagesFiles(def packages) {
		packages.each { name, pack ->
			mapper.writeValue(new File(packagesFolder, "${name}.bee"), pack)
		}
	}

	void createTriggersFiles(def triggers) {
		triggers.each { name, trigger ->
			mapper.writeValue(new File(triggersFolder, "${name}.bee"), trigger)
		}
	}
	
	void createUserTypesFiles(def userTypes) {
		userTypes.each { name, userType ->
			mapper.writeValue(new File(userTypesFolder, "${name}.bee"), userType)
		}
	}

}
