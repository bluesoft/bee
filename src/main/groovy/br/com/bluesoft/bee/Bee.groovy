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
package br.com.bluesoft.bee

import br.com.bluesoft.bee.data.BeeDataModule
import br.com.bluesoft.bee.dbchange.BeeDbChangeModule
import br.com.bluesoft.bee.dbseed.BeeDbSeedModule
import br.com.bluesoft.bee.exceptions.IncorrectUsageException
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.BeeModule
import br.com.bluesoft.bee.schema.BeeSchemaModule
import br.com.bluesoft.bee.upgrade.BeeUpgradeModule

import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

class Bee {

    static getRunner(options) {
        def runner = null

        switch (options.moduleName) {
            case "dbchange":
                runner = new BeeDbChangeModule()
                break;
            case "dbseed":
                runner = new BeeDbSeedModule()
                break;
            case "schema":
                runner = new BeeSchemaModule()
                break;
            case "data":
                runner = new BeeDataModule()
                break;
            case "upgrade":
                runner = new BeeUpgradeModule()
                break;
            default:
                options.usage()
                System.exit(0)
        }

        return runner
    }

    static getVersion() {
        def resources = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME)
        def version = "test"

        resources.each {
            try {
                Manifest manifest = new Manifest(it.openStream())
                if (manifest.mainAttributes[Attributes.Name.IMPLEMENTATION_TITLE] == 'bee') {
                    version = manifest.mainAttributes[Attributes.Name.IMPLEMENTATION_VERSION]
                }
            } catch (Exception e) {
                // silent ignore
            }
        }

        return version
    }

    static main(args) {
        def version = getVersion()
        println "Bee - v. ${version} - Bluesoft (2013-${Calendar.getInstance().get(Calendar.YEAR)}) - GPL - All rights reserved"
        Options options = Options.instance
        if (!options.parse(args)) {
            options.usage()
            System.exit(1)
        }
        BeeModule runner = getRunner(options)

        if (runner == null) {
            System.exit 0
        }

        try {
            runner.run(options)
        } catch (IncorrectUsageException e) {
            print 'Incorrect usage'
            System.exit 1
        }
    }
}
