package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbChangeMarkAllAction extends ActionRunnerParameterValidate {

    boolean run() {
        def clientName = options.arguments[0]
        def group = options.arguments[1]

        def manager = new DbChangeManager(configFile: Options.instance.configFile, path: Options.instance.dataDir.absolutePath, clientName: clientName, logger: out)
        manager.markAll(group)
        true
    }

    @Override
    int maxParameters() {
        return 1
    }
}
