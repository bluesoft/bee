package br.com.bluesoft.bee

import br.com.bluesoft.bee.model.Options;
import br.com.bluesoft.bee.service.BeeWriter;
import br.com.bluesoft.bee.importer.JsonImporter

class BeeMySqlSchemaCreator extends BeeSchemaCreator{

	def createColumn(def column) {
		def result = "    ${column.name} ${column.type}"
		if(column.type in ['char', 'varchar'])
			result += "(${column.size})"
		if(column.type == 'number')
			if(column.scale > 0)
				result += "(${column.size}, ${column.scale})"
			else
				result += "(${column.size})"

		if(column.defaultValue)
			result += " default ${column.defaultValue}"

		if(!column.nullable)
			result += ' not null'
		if(column.autoIncrement)
			result += ' auto_increment'
		return result
	}

	def createTable(def table) {
		def columns = []
		boolean createPrimaryKeyColumn = false
		String primaryKeyColumn = null
		
		table.columns.each({
			 columns << createColumn(it.value)
			 if (it.value.autoIncrement) {
				 createPrimaryKeyColumn = true
				 primaryKeyColumn = it.value.name
			 } 
		})
		
		if (createPrimaryKeyColumn)
			columns << "    primary key(" + primaryKeyColumn + ")"
		
		def result = "create table ${table.name} (\n" + columns.join(",\n") + "\n);\n\n"
	}
	
	def createPrimaryKey(table) {
		def constraint = table.constraints.find ({ it.value.type == 'P'})
		if(constraint == null)
			return ""

		constraint = constraint.value

		return "alter table ${table.name} add constraint ${constraint.name} primary key (" + constraint.columns.join(',') + ");\n"
	}
	
}
