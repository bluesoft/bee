package br.com.bluesoft.bee.runner

import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.service.BeeWriter

abstract class ActionRunnerParameterValidate implements ActionRunner {

    protected Options options
    protected BeeWriter out

    int minParameters() {
        return 1
    };

    int maxParameters() {
        return 2
    };

    @Override
    boolean validateParameters() {
        def min = minParameters()
        def max = maxParameters()

        return !(options.arguments.size > max || options.arguments.size < min)
    }
}
