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
package br.com.bluesoft.bee.importer

import br.com.bluesoft.bee.model.MView
import br.com.bluesoft.bee.model.Package
import br.com.bluesoft.bee.model.Procedure
import br.com.bluesoft.bee.model.rule.Rule
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.Sequence
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.Trigger
import br.com.bluesoft.bee.model.UserType
import br.com.bluesoft.bee.model.View
import br.com.bluesoft.bee.util.JsonUtil
import br.com.bluesoft.bee.util.RDBMS
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class JsonImporter implements Importer {

    def path
    ObjectMapper mapper
    File mainFolder
    File tablesFolder
    File sequencesFolder
    File viewsFolder
    File proceduresFolder
    File packagesFolder
    File triggersFolder
    File userTypesFolder
    File mviewsFolder
    File rulesFile

    JsonImporter() {
        this(null)
    }

    JsonImporter(def path) {
        this.path = path ?: '/tmp/bee'
        this.mainFolder = new File(this.path)
        this.tablesFolder = new File(mainFolder, 'tables')
        this.sequencesFolder = new File(mainFolder, 'sequences')
        this.viewsFolder = new File(mainFolder, 'views')
        this.proceduresFolder = new File(mainFolder, 'procedures')
        this.packagesFolder = new File(mainFolder, 'packages')
        this.triggersFolder = new File(mainFolder, 'triggers')
        this.userTypesFolder = new File(mainFolder, 'usertypes')
        this.mviewsFolder = new File(mainFolder, 'mviews')
        this.rulesFile = new File(mainFolder, "rules.json")

        this.mapper = JsonUtil.createMapper()
    }

    Schema importMetaData() {
        Schema schema = new Schema()
        schema.tables = importTables()
        schema.views = importViews()
        schema.sequences = importSequences()
        schema.procedures = importProcedures()
        schema.packages = importPackages()
        schema.triggers = importTriggers()
        schema.userTypes = importUserTypes()
        schema.mviews = importMViews()
        schema.rules = importRules();
        return schema
    }

    private def importTables() {
        checkIfFolderExists(tablesFolder)
        def tables = [:]
        tablesFolder.eachFile {
            if (it.name.endsWith(".bee")) {
                def table = mapper.readValue(it, Table.class)
                tables[table.name] = table
            }
        }
        return tables
    }

    private def importViews() {
        checkIfFolderExists(viewsFolder)
        def views = [:]
        def files = viewsFolder.listFiles().sort { it.name }
        files.each {
            if (it.name.endsWith(".bee")) {
                def view = mapper.readValue(it, View.class)
                views[view.name] = view
            }
        }
        return views
    }

    private def importSequences() {
        def sequences = [:]
        def sequencesFolderExists = checkIfFolderExists(sequencesFolder)

        if (sequencesFolderExists && sequencesFolder.listFiles().size() > 0) {
            sequencesFolder.eachFile {
                if (it.name.endsWith(".bee")) {
                    def sequence = mapper.readValue(it, Sequence.class)
                    sequences[sequence.name] = sequence
                }
            }
        } else {
            File sequenceFile = new File(mainFolder, 'sequences.bee')
            if (sequenceFile.exists()) {
                def sequencesJSON = mapper.readTree(sequenceFile.getText())
                sequencesJSON.elements().each {
                    def sequence = mapper.treeToValue(it, Sequence.class)
                    sequences[sequence.name] = sequence
                }
            }
        }

        return sequences
    }

    private def importProcedures() {
        checkIfFolderExists(proceduresFolder)
        def procedures = [:]
        proceduresFolder.eachFile {
            if (it.name.endsWith(".bee")) {
                def procedure = mapper.readValue(it, Procedure.class)
                if(procedure.schema)
                    procedures["${procedure.schema}.${procedure.name}"] = procedure
                else
                    procedures[procedure.name] = procedure
            }
        }

        return procedures
    }

    private def importPackages() {
        checkIfFolderExists(packagesFolder)
        def packages = [:]
        packagesFolder.eachFile {
            if (it.name.endsWith(".bee")) {
                def pack = mapper.readValue(it, Package.class)
                packages[pack.name] = pack
            }
        }
        return packages
    }

    private def importTriggers() {
        checkIfFolderExists(triggersFolder)
        def triggers = [:]
        triggersFolder.eachFile {
            if (it.name.endsWith(".bee")) {
                def trigger = mapper.readValue(it, Trigger.class)
                triggers[trigger.name] = trigger
            }
        }
        return triggers
    }

    private def importUserTypes() {
        checkIfFolderExists(userTypesFolder)
        def userTypes = [:]
        userTypesFolder.eachFile {
            if (it.name.endsWith(".bee")) {
                def userType = mapper.readValue(it, UserType.class)
                userTypes[userType.name] = userType
            }
        }
        return userTypes
    }

    private def importRules() {
        def rules = [:]
        if(!rulesFile.exists()) return
        JsonNode tree = mapper.readTree(rulesFile)
        tree.fields().forEachRemaining({
            def rdbms = RDBMS.getByName(it.key)
            if(rdbms)
                rules[rdbms] = mapper.treeToValue(it.value, Rule.class)
        })

        rules.size() > 0 ? rules : null
    }

    private def importMViews() {
        checkIfFolderExists(mviewsFolder)
        def mviews = [:]
        def files = mviewsFolder.listFiles().sort { it.name }
        files.each {
            if (it.name.endsWith(".bee")) {
                def mview = mapper.readValue(it, MView.class)
                mviews[mview.name] = mview
            }
        }
        return mviews
    }

    private def checkIfFolderExists(def directory) {
        def exists = false
        if (!directory.isDirectory()) {
            directory.mkdir()
        } else {
            exists = true
        }
        return exists
    }

}
