package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbChangeUpAction extends ActionRunnerParameterValidate {

    boolean run() {
        def clientName = options.arguments[0]
        def migrationIds = options.arguments.drop(1) as List<String>
        def manager = new DbChangeManager(configFile: options.configFile, path: options.dataDir.absolutePath, logger: out, clientName: clientName, quiet: options.quiet)
        def lista = migrationIds ?: manager.listar()
        return manager.executarVariasDbChanges(lista, UpDown.UP)
    }

    @Override
    int maxParameters() {
        return Integer.MAX_VALUE
    }
}
