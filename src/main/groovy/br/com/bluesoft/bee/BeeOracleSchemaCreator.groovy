package br.com.bluesoft.bee

import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.service.BeeWriter;
import br.com.bluesoft.bee.importer.JsonImporter

class BeeOracleSchemaCreator extends BeeSchemaCreator {

	void createCoreData(def file, def schema, def dataFolderPath) {
		file.append("alter session set nls_date_format = 'yyyy-mm-dd';\n\n", 'utf-8')
		super.createCoreData(file, schema, dataFolderPath)
	}
	
	void createTables(def file, def schema) {
		def tables = schema.tables.sort()
		tables.each( { file << createTable(it.value) })
	}
	
	def createTable(def table) {
		def columns = []
		table.columns.each({
			columns << createColumn(it.value)
		})
		def temp = table.temporary ? " global temporary" : ""
		def result = "create${temp} table ${table.name} (\n" + columns.join(",\n") + "\n);\n\n"
	}
	
	def createColumn(def column) {
		def result = "    ${column.name} ${column.type}"
		if(column.type in ['char', 'varchar'])
			if(column.sizeType != null)
				result += "(${column.size} ${column.sizeType})"
			else
				result += "(${column.size})"
				
		if(column.type == 'number')
			if (column.scale > 0) {
				result += "(${column.size}, ${column.scale})"
			} else if (column.scale != null && column.size != null && column.size != 22) {
				result += "(${column.size})"
			}

		if(column.defaultValue)
			result += " default ${column.defaultValue}"

		if(!column.nullable)
			result += ' not null'
		return result
	}
}
