package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.runner.BeeModule

public class BeeDbSeedModule extends BeeModule {

    def usage() {
        println "usage: bee <options> dbseed:action <parameters>"
        println "Actions:"
        println "         dbseed:create description - creates a dbseed file"
        println "         dbseed:status connection - lists dbseeds to run"
        println "         dbseed:up connection <file> - runs all pending dbseed files, or one if specified"
        println "         dbseed:down connection file - runs a dbseed rollback action"
        println "         dbseed:mark connection file - mark a file as executed"
        println "         dbseed:markall connection - mark All files as executed"
        println "         dbseed:unmark connection file - unmark a file as executed"
    }

    @Override
    protected ActionRunner getRunner(Object action, Object options, Object out) {
        switch (action) {
            case "create":
                return new BeeDbSeedCreateAction(options: options, out: this)
                break
            case "status":
                return new BeeDbSeedStatusAction(options: options, out: this)
                break
            case "up":
                return new BeeDbSeedUpAction(options: options, out: this)
                break
            case "down":
                return new BeeDbSeedDownAction(options: options, out: this)
                break
            case "mark":
                return new BeeDbSeedMarkAction(options: options, out: this)
                break
            case "markall":
                return new BeeDbSeedMarkAllAction(options: options, out: this)
                break
            case "unmark":
                return new BeeDbSeedUnmarkAction(options: options, out: this)
                break
        }
    }
}
