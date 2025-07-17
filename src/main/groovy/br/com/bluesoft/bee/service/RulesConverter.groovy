package br.com.bluesoft.bee.service

import br.com.bluesoft.bee.model.Constraint
import br.com.bluesoft.bee.model.Index
import br.com.bluesoft.bee.model.IndexColumn
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.TableColumn
import br.com.bluesoft.bee.model.rule.DataType
import br.com.bluesoft.bee.model.rule.Rule
import org.apache.groovy.util.Maps

import java.util.regex.Pattern

class RulesConverter {

    Map<String, String> checksConst = [:]
    List<String[]> checksRegex = []
    Map<String, Map<String, String>> colDefConst = [:]
    Map<String, List<String[]>> colDefRegex = [:]
    Map<String, String> indColConst = [:]
    Map<String, String> indFilterConst = [:]


    Schema toSchema(Schema source) {
        if(!source.rules?.containsKey(source.rdbms))
            return source

        Rule rule = source.rules[source.rdbms]
        prepareRules(rule.columnDefaultOut, rule.checkConditionOut, rule.indexColumnOut, rule.indexFilterOut)
        def schema = source.clone()
        schema.tables = source.tables.collectEntries { k, v -> [k, convertTable(v, rule.dataTypeOut)]}
        return schema
    }

    Schema fromSchema(Schema source) {
        if(!source.rules?.containsKey(source.rdbms))
            return source

        Rule rule = source.rules[source.rdbms]
        prepareRules(rule.columnDefaultIn, rule.checkConditionIn, rule.indexColumnIn, [:])
        def schema = source.clone()
        schema.tables = source.tables.collectEntries { k, v -> [k, convertTable(v, rule.dataTypeIn)]}
        return schema
    }

    Table convertTable(Table source, List<DataType> dataTypes) {
        def result = source.clone()
        result.columns = source.columns.collectEntries({ k, v ->
            def dataType = findDataType(v, dataTypes)
            def defaultValue = findDefaultValue(v)
            return [k, convertColumn(v, dataType, defaultValue)]
        })
        result.constraints = source.constraints.collectEntries { [it.key, convertCheckConstraint(it.value)] }
        result.indexes = source.indexes.collectEntries { [it.key, convertIndex(it.value)]}
        return result
    }

    Index convertIndex(Index source) {
        Index result = source
        if(source.columns*.name.intersect(indColConst.keySet()).size() > 0) {
            result = source.clone()
            result.columns = source.columns.collect({
                new IndexColumn(name: indColConst.getOrDefault(it.name, it.name) , descend: it.descend, include: it.include)
            }).findAll({it.name != ''})
        }

        if(result.name in indFilterConst) {
            result = result.clone()
            result.where = indFilterConst[result.name]
        }
        return result;
    }

    Constraint convertCheckConstraint(Constraint source) {
        if (source.type == 'C') {
            if (source.searchCondition in checksConst) {
                def result = source.clone()
                result.searchCondition = checksConst[source.searchCondition]
                return result
            } else {
                checksRegex.each {
                    if(source.searchCondition.matches(it.key)) {
                        def result = source.clone()
                        result.searchCondition = result.searchCondition.replaceAll(it.key, it.value)
                        return result
                    }
                }
            }
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

    String findDefaultValue(TableColumn column) {
        if(column.defaultValue == null) {
            return null
        }

        if(colDefConst[column.type]?[column.defaultValue]) {
            return colDefConst[column.type]?[column.defaultValue]
        }

        for(it in colDefRegex[column.type]) {
            if(column.defaultValue.matches(it[0])) {
                def r = column.defaultValue.replaceAll(it[0], it[1])
                return r
            }
        }
    }

    void prepareRules(Map<String, Map<String, String>> colDefRules, Map<String, String> checkRules, Map<String, String> indColRules, Map<String, String> indFilterOut) {
        colDefConst = colDefRules.collectEntries { k, v ->
            Map<String, String> c = v.findAll {!it.key.startsWith("~")}
            return [k, c]
        }
        colDefRegex = colDefRules.collectEntries { k, v ->
            def c = v.findAll { it.key.startsWith("~")}.collect { [it.key.substring(1), it.value] as String[]}
            return [k, c]
        }
        checksConst = checkRules.findAll { !it.key.startsWith("~")}
        checksRegex = checkRules.findAll { it.key.startsWith("~")}.collect { [it.key.substring(1), it.value]}

        indColConst = indColRules
        indFilterConst = indFilterOut
    }
}
