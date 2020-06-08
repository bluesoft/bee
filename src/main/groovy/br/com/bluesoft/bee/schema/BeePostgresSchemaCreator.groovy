package br.com.bluesoft.bee.schema

class BeePostgresSchemaCreator extends BeeSchemaCreator {

    def createColumn(def column) {
        def result = "    ${column.name} ${column.type}"
        if (column.type in ['character', 'character varying', 'text']) {
            if (column.size != null) {
                result += "(${column.size})"
            }
        }

        if (column.type in ['decimal', 'numeric', 'serial', 'bigserial']) {
            if (column.scale > 0) {
                result += "(${column.size}, ${column.scale})"
            } else if (column.size != null) {
                result += "(${column.size})"
            }
        }

        if (column.defaultValue) {
            if (column.virtual) {
                result += " generated always as ${column.defaultValue} stored"
            } else {
                result += " default ${column.defaultValue}"
            }
        }

        if (!column.nullable) {
            result += ' not null'
        }

        return result
    }

    def createIndex(tableName, index) {
        def result = "create"
        def indexType = getIndexType(index.type)
        if (index.unique) {
            result += ' unique'
        }
        if (index.columns.size() == 1 && index.columns[0].name.contains('(')) {
            result += " index ${index.name} on ${tableName} USING ${indexType} (" + index.columns[0].name + ");\n"
        } else {
            result += " index ${index.name} on ${tableName} USING ${indexType} (" + index.columns.join(',') + ");\n"
        }

        return result
    }

    def getIndexType(indexType) {
        if (indexType == 'b') {
            return ' btree'
        } else {
            return ' btree'
        }
    }

    void createIndexes(def file, def schema) {
        def tables = schema.tables.sort()
        tables.each {
            def table = it.value
            def indexes = table.indexes*.value.findAll { it.type == 'n' }
            def primaryKeys = table.constraints*.value.findAll { it.type == 'P' }
            def uniqueKeys = table.constraints*.value.findAll { it.type == 'U' }
            indexes.each {
                def indexName = it.name
                def existPrimaryKeyWithThisName = primaryKeys.findAll { it.name.equals(indexName) }.size() == 1
                def existUniqueKeyWithThisName = uniqueKeys.findAll { it.name.equals(indexName) }.size() == 1
                if (!existPrimaryKeyWithThisName && !existUniqueKeyWithThisName) {
                    file << createIndex(table.name, it)
                }
            }
        }

        file << "\n"
    }


    void createProcedures(def file, def schema) {
        schema.procedures*.value.sort().each {
            def procedure = "${it.text};\n\n"
            file.append(procedure.toString(), 'utf-8')
        }
    }

    void createTriggers(def file, def schema) {
        schema.triggers*.value.sort().each {
            def trigger = "${it.text};\n\n"
            file.append(trigger.toString(), 'utf-8')
        }
    }

}
