package br.com.bluesoft.bee.service

import br.com.bluesoft.bee.model.Constraint
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.TableColumn
import br.com.bluesoft.bee.model.rule.DataType
import br.com.bluesoft.bee.model.rule.Rule

class RulesConverter {

    Schema toSchema(Schema source) {
        if(!source.rules?.containsKey(source.rdbms))
            return source

        Rule rule = source.rules[source.rdbms]
        def schema = source.clone()
        schema.tables = source.tables.collectEntries { k, v -> [k, convertTable(v, rule.dataTypeOut, rule.columnDefaultOut, rule.checkConditionOut)]}
        return schema
    }

    Schema fromSchema(Schema source) {
        if(!source.rules?.containsKey(source.rdbms))
            return source

        Rule rule = source.rules[source.rdbms]
        def schema = source.clone()
        schema.tables = source.tables.collectEntries { k, v -> [k, convertTable(v, rule.dataTypeIn, rule.columnDefaultIn, rule.checkConditionIn)]}
        return schema
    }

    Table convertTable(Table source, List<DataType> dataTypes, Map<String, Map<String, String>> columnDefaults, Map<String, String> checks) {
        def result = source.clone()
        result.columns = source.columns.collectEntries({ k, v ->
            def dataType = findDataType(v, dataTypes)
            return [k, convertColumn(v, dataType, columnDefaults[v.type]?[v.defaultValue])]
        })
        result.constraints = source.constraints.collectEntries { [it.key, convertCheckConstraint(it.value, checks)] }
        return result
    }

    Constraint convertCheckConstraint(Constraint source, Map<String, String> checks) {
        if(source.type == 'C' && source.searchCondition in checks) {
            def result = source.clone()
            result.searchCondition = checks[source.searchCondition]
            return result
        }
        return source
    }

    def convertColumn(TableColumn column, DataType dataType, String defaultValue) {
        if(dataType == null && defaultValue == null) return column
        TableColumn result = column.clone()
        if(dataType != null) {
            if(dataType.toType != null) {
                result.type = dataType.toType
            }
            if(dataType.toSize >= 0) {
                result.size = dataType.toSize
            }
            if(dataType.toScale >= 0) {
                result.scale = dataType.toScale
            }
        }
        if(defaultValue != null) {
            result.defaultValue = defaultValue
        }
        return result
    }

    DataType findDataType(TableColumn column, List<DataType> dataType) {
        dataType.find {
            if (column.type != it.fromType) return false
            if ((it.fromSize ?: 0) >= 0 && column.size != it.fromSize) return false
            if ((it.fromScale ?: 0) >= 0 && column.scale != it.fromScale) return false

            return true
        }
    }
}
