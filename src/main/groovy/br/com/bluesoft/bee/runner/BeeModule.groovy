package br.com.bluesoft.bee.runner

import br.com.bluesoft.bee.service.BeeWriter

abstract class BeeModule implements BeeWriter {

    abstract def usage()

    def parseOptions(options) {
        def action = options.actionName
        def actionRunner = getRunner(action, options, this);

        if (!actionRunner.validateParameters()) {
            usage();
            System.exit 0
        }

        return actionRunner
    }

    protected abstract ActionRunner getRunner(action, options, out);

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
