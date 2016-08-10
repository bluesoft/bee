package br.com.bluesoft.bee.schema;

import br.com.bluesoft.bee.importer.JsonImporter
import br.com.bluesoft.bee.model.Options
import br.com.bluesoft.bee.service.BeeWriter

public class BeeSchemaCheckerAction {
	Options options
	BeeWriter out

	def importer

	public boolean validateParameters() {
		return true
	}

	def run() {
		out.log('importing schema metadata from the reference files')
		def schema = getImporter().importMetaData()
		def errors = []

		validateForeignKeys(schema, errors)

		if(errors.size > 0) {
			errors.each { println it }
		}

		return errors.size != 0
	}

	def validateForeignKeys(def schema, def errors) {
		schema.tables.each {
			def table = it.value
			table.constraints.each {
				if(it.value.type == "R")
					validateForeignKey(table, it.value, schema, errors)
			}
		}
	}

	private def validateForeignKey(def table, def constraint, def schema, def errors) {
		def columns = []
		constraint.columns.each {
			columns << table.columns[it]
		}

		def rtable = schema.tables[constraint.refTable]
		def rpk = rtable.constraints.find {	it.value.type == "P" }.value
		def rcolumns = []
		rpk.columns.each { rcolumns << rtable.columns[it] }

		if(columns.size() != rcolumns.size()) {
			errors << "Foreign Key: The number of columns differs from ${table.name}(${columns.size()}) and ${rtable.name}(${rcolumns.size()})"
			return
		}

		def pos = 0
		columns.each {
			if(!it.compareType(rcolumns[pos++])) {
				errors << "Foreign Key: The column ${table.name}.${it.name} in the constraint ${constraint.name} differs from referenced table ${rtable.name}"
			}
		}
	}

	private def getImporter() {
		if(importer == null)
			return new JsonImporter(options.dataDir.canonicalPath)
		return importer
	}
}
