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

import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.runner.BeeModule

public class BeeDbChangeModule extends BeeModule {

    def usage() {
        println "usage: bee <options> dbchange:action <parameters>"
        println "Actions:"
        println "         dbchange:create description <group> - creates a dbchange file"
        println "         dbchange:status connection <group> - lists dbchanges to run"
        println "         dbchange:up connection <file> - runs all pending dbchange files, or one if specified"
        println "         dbchange:down connection file - runs a dbchange rollback action"
        println "         dbchange:mark connection file - mark a file as executed"
        println "         dbchange:markall connection <group> - mark all files as executed"
        println "         dbchange:unmark connection file - unmark a file as executed"
        println "         dbchange:find name - search file"
        println "         dbchange:open name - open file with EDITOR"
        println "         dbchange:group_up connection group - runs all pending dbchange files of the group"
    }

    @Override
    protected ActionRunner getRunner(Object action, Object options, Object out) {
        switch (action) {
            case "create":
                return new BeeDbChangeCreateAction(options: options, out: this)
                break
            case "status":
                return new BeeDbChangeStatusAction(options: options, out: this)
                break
            case "up":
                return new BeeDbChangeUpAction(options: options, out: this)
                break
            case "down":
                return new BeeDbChangeDownAction(options: options, out: this)
                break
            case "mark":
                return new BeeDbChangeMarkAction(options: options, out: this)
                break
            case "markall":
                return new BeeDbChangeMarkAllAction(options: options, out: this)
                break
            case "unmark":
                return new BeeDbChangeUnmarkAction(options: options, out: this)
                break
            case "find":
                return new BeeDbChangeFindAction(options: options, out: this)
                break
            case "open":
                return new BeeDbChangeOpenAction(options: options, out: this)
                break
            case "group_up":
                return new BeeDbChangeGroupUpAction(options: options, out: this)
                break
        }
    }

}
