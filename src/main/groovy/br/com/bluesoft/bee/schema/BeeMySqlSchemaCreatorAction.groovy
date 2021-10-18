package br.com.bluesoft.bee.schema

import br.com.bluesoft.bee.importer.JsonImporter
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.service.RulesConverter
import br.com.bluesoft.bee.util.RDBMS

class BeeMySqlSchemaCreatorAction implements ActionRunner {

    Options options
    BeeWriter out
    BeeSchemaCreator beeSchemaCreator

    def importer

    //TODO types, outputfile

    public boolean validateParameters() {
        return true
    }

    public boolean run() {

        def objectName = options.arguments[0]

        out.log('importing schema metadata from the reference files')
        def schema = getImporter().importMetaData()
        schema.rdbms = RDBMS.MYSQL
        schema = new RulesConverter().toSchema(schema)

		if (objectName) {
			schema = schema.filter(objectName)
		}

        def file = new File('bee.sql')
		if (file.exists()) {
			file.delete()
		}

        beeSchemaCreator = new BeeMySqlSchemaCreator()

        out.println("generating tables...")
        beeSchemaCreator.createTables(file, schema)

        out.println("generating constraints...")
        beeSchemaCreator.createPrimaryKeys(file, schema)
        beeSchemaCreator.createUniqueKeys(file, schema)
        beeSchemaCreator.createForeignKeys(file, schema)

        out.println("generating indexes...")
        beeSchemaCreator.createIndexes(file, schema)
        beeSchemaCreator.createFunctionalIndexes(file, schema)
        beeSchemaCreator.createBitmapIndexes(file, schema)

        out.println("generating views...")
        beeSchemaCreator.createViews(file, schema)

        out.println("generating packages...")
        beeSchemaCreator.createPackages(file, schema)

        out.println("generating procedures...")
        beeSchemaCreator.createProcedures(file, schema)

        out.println("generating triggers...")
        beeSchemaCreator.createTriggers(file, schema)

        def env = System.getenv()
        if (env['EDITOR']) {
            println "Opening editor ${env['EDITOR']}"
            def cmd = [env['EDITOR'], file.path]
            new ProcessBuilder(env['EDITOR'], file.path).start()
        }
        return true
    }

    private def getImporter() {
		if (importer == null) {
			return new JsonImporter(options.dataDir.canonicalPath)
		}
        return importer
    }

}
