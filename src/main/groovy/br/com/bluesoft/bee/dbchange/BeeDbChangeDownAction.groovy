package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbChangeDownAction extends ActionRunnerParameterValidate {

    boolean run() {
        def clientName = options.arguments[0]
        def migrationId = options.arguments[1]

        def manager = new DbChangeManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out, clientName: clientName)
        manager.executarDbChange(migrationId, UpDown.DOWN)
        true
    }

    @Override
    int minParameters() {
        return 2
    }
}
