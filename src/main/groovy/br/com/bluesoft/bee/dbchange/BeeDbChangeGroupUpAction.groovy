package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbChangeGroupUpAction extends ActionRunnerParameterValidate {

    boolean run() {
        def clientName = options.arguments[0]
        def migrationGroup = options.arguments[1]

        def manager = new DbChangeManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out, clientName: clientName)
        def lista = manager.listar(migrationGroup)
        manager.executarVariasDbChanges(lista, UpDown.UP)
        true
    }

    @Override
    int minParameters() {
        return 2
    }
}
