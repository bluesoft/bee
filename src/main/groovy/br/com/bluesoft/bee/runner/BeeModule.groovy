package br.com.bluesoft.bee.runner

import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter

abstract class BeeModule implements BeeWriter {

    abstract def usage()

    protected abstract ActionRunner getRunner(String action, Options options, BeeWriter out);

    def parseOptions(options) {
        def action = options.actionName
        def actionRunner = getRunner(action, options, this);

        if (!actionRunner.validateParameters()) {
            usage()
            System.exit 0
        }

        return actionRunner
    }

    def run(options) {
        def actionRunner = parseOptions(options)
        if (actionRunner) {
            if (!actionRunner.run()) {
                System.exit(1)
            }
        }
    }

    void log(String msg) {
        println msg
    }
}
