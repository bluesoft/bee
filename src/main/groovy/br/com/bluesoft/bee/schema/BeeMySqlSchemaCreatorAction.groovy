package br.com.bluesoft.bee.schema

import br.com.bluesoft.bee.importer.JsonImporter;
import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.service.BeeWriter;


class BeeMySqlSchemaCreatorAction {

	Options options
	BeeWriter out
	BeeSchemaCreator beeSchemaCreator

	def importer
	String outputfile
	//TODO types

	public boolean validateParameters() {
		return true
	}

	public void run() {

		def objectName = options.arguments[0]

		out.log('importing schema metadata from the reference files')
		def schema = getImporter().importMetaData()

		if(objectName)
			schema = schema.filter(objectName)

		def file = new File(outputfile)
		if(file.exists())
			file.delete()

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
		if(env['EDITOR']) {
			println "Opening editor ${env['EDITOR']}"
			def cmd = [env['EDITOR'], file.path]
			new ProcessBuilder(env['EDITOR'], file.path).start()
		}
	}

	private def getImporter() {
		if(importer == null)
			return new JsonImporter(options.dataDir.canonicalPath)
		return importer
	}

}
