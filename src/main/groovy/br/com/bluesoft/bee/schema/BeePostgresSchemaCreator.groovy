package br.com.bluesoft.bee.schema

import br.com.bluesoft.bee.util.RDBMS

class BeePostgresSchemaCreator extends BeeSchemaCreator {

    def createColumn(def column) {
        def result = "    ${column.name} ${column.type}"
        if (column.type in ['character', 'character varying']) {
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
                result += " generated always as (${column.defaultValue}) stored"
            } else {
                result += " default ${column.defaultValue}"
            }
        }

        if (!column.nullable) {
            result += ' not null'
        }

        return result
    }

    def createForeignKey(table) {
        def constraints = table.constraints.findAll { it.value.type == 'R' }*.value

        def result = ""

        constraints.each {
            def onDelete = it.onDelete ? " on delete ${it.onDelete}" : ""
            def onUpdate = it.onUpdate ? " on update ${it.onUpdate}" : ""
            def refColumns = it.refColumns ? "(" + it.refColumns.join(',') + ")" : ""
            result += "alter table ${table.name} add constraint ${it.name} foreign key (" + it.columns.join(',') + ") references ${it.refTable}${refColumns}${onDelete}${onUpdate};\n"
        }

        return result
    }

    def createIndex(tableName, index) {
        def result = "create"
        def indexType = getIndexType(index.type)
        if (index.unique) {
            result += ' unique'
        }

        result += " index ${index.name} on ${tableName} USING ${indexType} (" + index.columns.join(',') + ")"

        if(index.where) {
            result += " where ${index.where}"
        }

        result += ";\n"

        return result
    }

    def getIndexType(indexType) {
        if (indexType == 'b') {
            return ' btree'
        } else {
            return ' btree'
        }
    }

    void createCoreData(def file, def schema, def dataFolderPath) {
        if (!schema.filtered) {
            super.createCoreData(file, schema, dataFolderPath, false)
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
            def procedure = "${it.getCanonical(schema.rdbms).text};\n\n"
            file.append(procedure.toString(), 'utf-8')
        }
    }

    void createTriggers(def file, def schema) {
        schema.triggers*.value.sort().each {
            def trigger = "${it.getCanonical(schema.rdbms).text};\n\n"
            file.append(trigger.toString(), 'utf-8')
        }
    }

    String toBoolean(String fieldValue) {
        if(fieldValue == '0') return 'false'
        if(fieldValue == '1') return 'true'
        fieldValue
    }
}
