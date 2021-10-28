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
package br.com.bluesoft.bee.model;

public class Options {

    File configFile
    File dataDir
    List<String> arguments = []
    String moduleName
    String actionName
    CliBuilder cliBuilder

    private static final INSTANCE = new Options()

    static getInstance() {
        return INSTANCE
    }

    private Options() {
        cliBuilder = getCliBuilder()
    }

    private def getCliBuilder() {
        def cliBuilder = new CliBuilder(usage: 'bee <options> module:action <parameters>', header: 'Options:')
        cliBuilder.c(args: 1, argName: 'file', 'config file')
        cliBuilder.d(args: 1, argName: 'dir', 'bee files directory')
        cliBuilder.stopAtNonOption = false
        return cliBuilder
    }

    void usage() {
        cliBuilder.usage()
        println "Modules: "
        println "         schema"
        println "         data"
        println "         dbchange"
        println "         dbseed"
        println "         upgrade"
    }

    boolean parse(def args) {
        def cli = cliBuilder.parse(args)

        if (cli == null || cli == false || cli.arguments().size() < 1) {
            return false;
        }

        def module = cli.arguments()[0]
        if (!parseModule(module)) {
            return false;
        }

        if (cli.c) {
            configFile = new File(cli.c)
        } else {
            configFile = new File("bee.properties")
        }

        if (cli.d) {
            dataDir = new File(cli.d)
        } else {
            dataDir = new File("bee")
        }

        arguments = cli.arguments()
        arguments.remove(0)

        return validate()
    }

    private boolean parseModule(def module) {
        if (module.contains("upgrade")) {
            def args = module.split(":")
            moduleName = args[0]

            if (args.size() > 1) {
                actionName = (args[1] == "y") ? args[1] : null
            }

            return true
        }

        if (!module.contains(":")) {
            return false
        }

        moduleName = module.split(":")[0]
        actionName = module.split(":")[1]
    }

    private boolean validate() {

        if (!configFile.isFile()) {
            println "Could not read config file: ${configFile}"
        }

        if (!dataDir.isDirectory()) {
            println "Cound not read data dir: ${dataDir}"
        }

        return configFile.isFile() && dataDir.isDirectory()
    }
}
