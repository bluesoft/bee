package br.com.bluesoft.bee.runner

import br.com.bluesoft.bee.service.BeeWriter

abstract class ActionRunnerMultipleParameter implements ActionRunner {

    abstract boolean execute(params);

    BeeWriter out
    def options

    @Override
    boolean run() {
        def arguments = options.arguments

        for (int i = 1; i < arguments.size(); i++) {
            def params = [arguments[0], arguments[i]]
            def result = execute(params)
            if (!result) {
                return false
            }
        }

        return true
    }
}
