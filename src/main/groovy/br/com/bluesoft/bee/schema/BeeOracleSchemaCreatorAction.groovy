package br.com.bluesoft.bee.schema

import br.com.bluesoft.bee.importer.BeeImporter
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.service.RulesConverter
import br.com.bluesoft.bee.util.RDBMS

public class BeeOracleSchemaCreatorAction implements ActionRunner {

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
        def dataFolderPath = new File(options.dataDir.absolutePath, 'data')

        out.log('importing schema metadata from the reference files')
        def schema = getImporter().importMetaData()
        schema.rdbms = RDBMS.ORACLE

        if (objectName) {
            schema = schema.filter(objectName)
        }
        schema = new RulesConverter().toSchema(schema)

        def file = new File('bee.sql')
        if (file.exists()) {
            file.delete()
        }

        beeSchemaCreator = new BeeOracleSchemaCreator()

        out.println("generating sequences...")
        beeSchemaCreator.createSequences(file, schema)

        out.println("generating tables...")
        beeSchemaCreator.createTables(file, schema)

        out.println("generating core data...")
        beeSchemaCreator.createCoreData(file, schema, options.dataDir.absolutePath)

        out.println("generating constraints...")
        beeSchemaCreator.createPrimaryKeys(file, schema)
        beeSchemaCreator.createUniqueKeys(file, schema)
        beeSchemaCreator.createForeignKeys(file, schema)
        beeSchemaCreator.createCheckConstraint(file, schema)

        out.println("generating indexes...")
        beeSchemaCreator.createIndexes(file, schema)
        beeSchemaCreator.createFunctionalIndexes(file, schema)
        beeSchemaCreator.createBitmapIndexes(file, schema)

        out.println("generating views...")
        beeSchemaCreator.createViews(file, schema)

        out.println("generating user types...")
        beeSchemaCreator.createUserTypes(file, schema)

        out.println("generating packages...")
        beeSchemaCreator.createPackages(file, schema)

        out.println("generating procedures...")
        beeSchemaCreator.createProcedures(file, schema)

        out.println("generating materialized views...")
        beeSchemaCreator.createMViews(file, schema)

        out.println("generating materialized views indexes...")
        beeSchemaCreator.createMViewIndexes(file, schema)

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
            return new BeeImporter(options.dataDir.canonicalPath)
        }
        return importer
    }
}

