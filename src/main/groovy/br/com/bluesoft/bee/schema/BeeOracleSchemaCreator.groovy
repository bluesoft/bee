package br.com.bluesoft.bee.schema

import br.com.bluesoft.bee.util.StringUtil;


class BeeOracleSchemaCreator extends BeeSchemaCreator {

	void createCoreData(def file, def schema, def dataFolderPath) {
		file.append("alter session set nls_date_format = 'yyyy-mm-dd';\n", 'utf-8')
		file.append("set define off;\n\n", 'utf-8')
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

		if(column.virtual) {
			result += " as (${column.defaultValue})"
		} else {
			if(column.defaultValue)
				result += " default ${column.defaultValue}"
		}

		if(!column.nullable)
			result += ' not null'
		return result
	}

	def createForeignKey(table) {
		def constraints = table.constraints.findAll { it.value.type == 'R' }*.value

		def result = ""

		constraints.each {
			def onDelete = it.onDelete ? "on delete ${it.onDelete} " : ""
			def refColumns = it.refColumns ? "(" + it.refColumns.join(',') + ") " : ""
			result += "alter table ${table.name} add constraint ${it.name} foreign key (" + it.columns.join(',') + ") references ${it.refTable} ${refColumns}${onDelete}${it.status};\n"
		}

		return result
	}

	def createUserTypes(def file, def schema) {
		schema.userTypes.sort().each {
			def userType = StringUtil.deleteSchemaNameFromUserTypeText(it.value.text)
			userType += "/\n"
			file.append(userType.toString(), 'utf-8')
		}
	}
	
}
