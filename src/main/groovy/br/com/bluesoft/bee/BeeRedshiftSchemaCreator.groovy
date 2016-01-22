package br.com.bluesoft.bee


class BeeRedshiftSchemaCreator extends BeeSchemaCreator {

	def createColumn(def column) {
		def result = "    ${column.name} ${column.type}"
		if (column.type in ['character', 'character varying', 'text']) {
			if (column.size != null) {
				result += "(${column.size})"
			}
		}

		if (column.type in ['decimal','numeric','serial', 'bigserial']) {
			if (column.scale > 0) {
				result += "(${column.size}, ${column.scale})"
			} else if (column.size != null) {
				result += "(${column.size})"
			}
		}

		if(column.defaultValue)
			result += " default ${column.defaultValue}"

		if (!column.nullable) {
			result += ' not null'
		}

		return result
	}


}
