package br.com.bluesoft.bee.schema

import br.com.bluesoft.bee.importer.BeeImporter
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.service.RulesConverter
import br.com.bluesoft.bee.util.RDBMS

public class BeeRedshiftSchemaCreatorAction implements ActionRunner {

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
        def schema = cleanupSchema(getImporter().importMetaData())
        schema.rdbms = RDBMS.REDSHIFT

        if (objectName) {
            schema = schema.filter(objectName)
        }
        schema = new RulesConverter().toSchema(schema)

        def file = new File('bee.sql')
        if (file.exists()) {
            file.delete()
        }

        beeSchemaCreator = new BeeRedshiftSchemaCreator()

        out.println("generating tables...")
        beeSchemaCreator.createTables(file, schema)

        out.println("generating core data...")
        beeSchemaCreator.createCoreData(file, schema, options.dataDir)

        out.println("generating constraints...")
//        beeSchemaCreator.createPrimaryKeys(file, schema)

        out.println("generating views...")
        beeSchemaCreator.createViews(file, schema)

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

    Schema cleanupSchema(Schema schema) {
        schema.userTypes.clear()
        schema.packages.clear()
        schema.tables = schema.tables.findAll { !it.value.temporary }
        return schema
    }

}

