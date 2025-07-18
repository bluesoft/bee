package br.com.bluesoft.bee.schema

import br.com.bluesoft.bee.model.Index
import br.com.bluesoft.bee.util.CsvUtil

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

    def createIndex(tableName, Index index) {
        def result = "create"
        def indexType = getIndexType(index.type)
        if (index.unique) {
            result += ' unique'
        }

        result += " index ${index.name} on ${tableName} USING ${indexType} (" + index.columns.findAll { it -> !it.include }.join(',') + ")"

        if (index.where) {
            result += " where ${index.where}"
        }

        def includeColumns = index.columns.findAll { it.include }.collect { it.name }.join(',')
        if (includeColumns) {
            result += " INCLUDE (" + includeColumns + ")"
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

    void createCsvData(def file, def csvFile, def schema, def useCommit) {
        def tableName = csvFile.name.split('\\.')[0]
        def fileData = CsvUtil.read(csvFile)
        def table = schema.tables[tableName]
        def columnNames = []
        def columns = [:]
        def columnTypes = [:]
        def isVirtualColumn = [:]
        def numberOfVirtualColumns = 0

        if (table != null) {
            table.columns.findAll { !it.value.ignore }.each {
                columns[it.value.name] = it.value.type
                columnNames << it.value.name
                isVirtualColumn[it.value.name] = it.value.virtual
                if (it.value.virtual) {
                    numberOfVirtualColumns++
                }
            }

            columnNames.eachWithIndex { columName, index ->
                columnTypes[index] = columns[columName]
            }

            def counterValue = 1

            def query = new StringBuilder()
            query << "copy ${tableName} (${table.columns.findAll({!it.value.virtual && !it.value.ignore})*.value.name.join(",")}) from stdin;\n"

            for (int i = 0; i < fileData.size(); i++) {
                fileData[i].eachWithIndex { columnValue, index2 ->
                    def fieldValue = columnValue.toString().replace('\t', '\\t').replace('\\', '\\\\')
                    def columnType = columnTypes[index2].split(' ')[0].split('\\(')[0]
                    def columnName = columnNames[index2]
                    def isVirtual = isVirtualColumn[columnName]
                    def isBoolean = columnType == 'boolean'
                    if (!isVirtual) {
                        if(isBoolean) {
                            fieldValue = toBoolean(fieldValue)
                        }
                        if(fieldValue == "null") {
                            fieldValue = '\\N'
                        }
                        query << fieldValue
                    }
                    if ((counterValue + numberOfVirtualColumns) < (columnNames.size())) {
                        query << "\t"
                    }
                    counterValue++
                }
                query << "\n"
                counterValue = 1
            }

            query << "\\.\n\n"

            if(useCommit) {
                query << "commit;\n"
            }
            file.append(query.toString(), 'utf-8')
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
