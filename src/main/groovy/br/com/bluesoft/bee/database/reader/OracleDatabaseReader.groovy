package br.com.bluesoft.bee.database.reader

import br.com.bluesoft.bee.model.Constraint
import br.com.bluesoft.bee.model.Index
import br.com.bluesoft.bee.model.IndexColumn
import br.com.bluesoft.bee.model.MView
import br.com.bluesoft.bee.model.MViewIndex
import br.com.bluesoft.bee.model.MViewIndexColumn
import br.com.bluesoft.bee.model.Package
import br.com.bluesoft.bee.model.Procedure
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.Sequence
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.TableColumn
import br.com.bluesoft.bee.model.Trigger
import br.com.bluesoft.bee.model.UserType
import br.com.bluesoft.bee.model.View
import br.com.bluesoft.bee.util.RDBMS
import groovy.sql.GroovyRowResult
import groovy.sql.Sql

class OracleDatabaseReader implements DatabaseReader {

    Sql sql

    OracleDatabaseReader(def sql) {
        this.sql = sql
    }

    Schema getSchema(objectName = null) {
        def schema = new Schema()
        schema.tables = getTables(objectName)
        schema.sequences = getSequences(objectName)
        schema.views = getViews(objectName)
        schema.procedures = getProcedures(objectName)
        schema.packages = getPackages(objectName)
        schema.triggers = getTriggers(objectName)
        schema.userTypes = getUserTypes(objectName)
        schema.mviews = getMViews(objectName)
        return schema
    }


    def getTables(objectName) {
        def tables = fillTables(objectName)
        fillColumns(tables, objectName)
        fillIndexes(tables, objectName)
        fillIndexColumns(tables, objectName)
        fillCostraints(tables, objectName)
        fillCostraintsColumns(tables, objectName)
        return tables
    }

    static final def TABLES_QUERY = '''
        select ut.table_name, ut.temporary, utc.comments
        from user_tables ut
        left join user_mviews mv on ut.table_name = mv.mview_name
        left join user_tab_comments utc on ut.table_name = utc.table_name
        where mv.mview_name is null
        order by table_name
	'''
    static final def TABLES_QUERY_BY_NAME = '''
        select ut.table_name, ut.temporary, utc.comments
        from user_tables ut
        left join user_mviews mv on ut.table_name = mv.mview_name
        left join user_tab_comments utc on ut.table_name = utc.table_name
        where mv.mview_name is null
		  and ut.table_name = upper(?)
		order by table_name
	'''

    private def fillTables(objectName) {
        def tables = [:]
        def rows
        if (objectName) {
            rows = sql.rows(TABLES_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(TABLES_QUERY)
        }

        rows.each({
            def name = it.table_name.toLowerCase()
            def temporary = it.temporary == 'Y' ? true : false
            def comment = it.comments
            tables[name] = new Table(name: name, temporary: temporary, comment: comment)
        })
        return tables
    }


    static final def TABLES_COLUMNS_QUERY = '''
        select ut.table_name, utc.column_name, data_type, nullable,
               coalesce(data_precision, to_number(decode(char_length, 0, data_length, char_length))) data_size,
                     data_precision, data_length, char_used as size_type,
               coalesce(data_scale, 0) data_scale, data_default, column_id, virtual_column, comments
        from   user_tab_cols utc 
          join user_tables ut on utc.table_name = ut.table_name
          left join user_col_comments ucm on utc.table_name = ucm.table_name and utc.column_name = ucm.column_name
          left join user_mviews uv on ut.table_name = uv.mview_name
        where  hidden_column = 'NO'
          and  uv.mview_name is null
        order  by table_name, column_id
		'''
    static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
        select ut.table_name, utc.column_name, data_type, nullable,
               coalesce(data_precision, to_number(decode(char_length, 0, data_length, char_length))) data_size,
                     data_precision, data_length, char_used as size_type,
               coalesce(data_scale, 0) data_scale, data_default, column_id, virtual_column, comments
        from   user_tab_cols utc 
          join user_tables ut on utc.table_name = ut.table_name
          left join user_col_comments ucm on utc.table_name = ucm.table_name and utc.column_name = ucm.column_name
          left join user_mviews uv on ut.table_name = uv.mview_name
        where  hidden_column = 'NO'
          and  uv.mview_name is null
          and  ut.table_name = upper(?)
		order  by table_name, column_id
	'''

    private def fillColumns(tables, objectName) {
        def rows
        if (objectName) {
            rows = sql.rows(TABLES_COLUMNS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(TABLES_COLUMNS_QUERY)
        }
        rows.each({
            def table = tables[it.table_name.toLowerCase()]
            def column = new TableColumn()
            column.name = it.column_name.toLowerCase()
            column.scale = it.data_scale == null ? 0 : it.data_scale
            column.type = getColumnType(it.data_type)
            column.size = getColumnSize(it)
            column.sizeType = getColumnSizeType(it.size_type)
            column.nullable = it.nullable == 'N' ? false : true
            column.virtual = it.virtual_column == 'YES'
            column.comment = it.comments
            def defaultValue = it.data_default
            if (defaultValue) {
                column.defaultValue = defaultValue?.trim()?.toUpperCase() == 'NULL' ? null : defaultValue?.trim()
            }
            table.columns[column.name] = column
        })
    }

    private def getColumnType(String oracleColumnType) {
        switch (oracleColumnType.toLowerCase()) {
            case "varchar2":
                return "varchar"
                break
            default:
                return oracleColumnType.toLowerCase()
        }
    }

    private def getColumnSizeType(String sizeType) {
        if (sizeType == "B") {
            return "BYTE"
        } else if (sizeType == "C") {
            return "CHAR"
        } else {
            return null
        }
    }

    private def getColumnSize(iterator) {
        if (isNumericWithoutSizeEspecified(iterator)) {
            return null
        } else {
            return iterator.data_size
        }
    }

    private def isNumericWithoutSizeEspecified(iterator) {
        return iterator.data_type.toUpperCase() == 'NUMBER' && iterator.data_precision == null && iterator.data_scale == 0 && iterator.data_length == 22
    }

    final static def INDEXES_QUERY = '''
        select ui.table_name, ui.index_name, ui.index_type, uniqueness
        from   user_indexes ui
          left join user_constraints uc on ui.index_name = uc.index_name
          left join user_mviews uv on ui.table_name = uv.mview_name
        where  (uc.index_name is null or (uc.index_name is not null and uc.index_name != uc.constraint_name))
          and index_type <> 'LOB'
          and uv.mview_name is null
        order  by table_name, index_name
	'''
    final static def INDEXES_QUERY_BY_NAME = '''
        select ui.table_name, ui.index_name, ui.index_type, uniqueness
        from   user_indexes ui
          left join user_constraints uc on ui.index_name = uc.index_name
          left join user_mviews uv on ui.table_name = uv.mview_name
        where  (uc.index_name is null or (uc.index_name is not null and uc.index_name != uc.constraint_name))
          and index_type <> 'LOB'
          and uv.mview_name is null
		  and  ui.table_name = upper(?)
		order  by table_name, index_name
	'''

    private def fillIndexes(tables, objectName) {
        def rows
        if (objectName) {
            rows = sql.rows(INDEXES_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(INDEXES_QUERY)
        }

        rows.each({
            def tableName = it.table_name.toLowerCase()
            def table = tables[tableName]
            def index = new Index()
            index.name = it.index_name.toLowerCase()
            index.type = it.index_type[0].toLowerCase()
            index.unique = it.uniqueness == 'UNIQUE' ? true : false
            table.indexes[index.name] = index
        })
    }

    final static def INDEXES_COLUMNS_QUERY = '''
        select uic.table_name, uic.index_name, uic.column_name column_name, utc.data_default,
               uic.descend
        from   user_ind_columns uic
               join user_indexes ui on ui.index_name = uic.index_name
               left join user_constraints uc on uic.index_name = uc.index_name
               join user_tab_cols utc on (uic.column_name = utc.column_name and uic.table_name = utc.table_name)
               left join user_mviews uv on uic.table_name = uv.mview_name
        where  (uc.index_name is null or (uc.index_name is not null and uc.index_name != uc.constraint_name))
          and  uv.mview_name is null
        order  by uic.index_name, uic.column_position
	'''
    final static def INDEXES_COLUMNS_QUERY_BY_NAME = '''
        select uic.table_name, uic.index_name, uic.column_name column_name, utc.data_default,
               uic.descend
        from   user_ind_columns uic
               join user_indexes ui on ui.index_name = uic.index_name
               left join user_constraints uc on uic.index_name = uc.index_name
               join user_tab_cols utc on (uic.column_name = utc.column_name and uic.table_name = utc.table_name)
               left join user_mviews uv on uic.table_name = uv.mview_name
        where  (uc.index_name is null or (uc.index_name is not null and uc.index_name != uc.constraint_name))
          and  uv.mview_name is null
		  and  ui.table_name = upper(?)
		order  by uic.index_name, uic.column_position
	'''

    private def fillIndexColumns(tables, objectName) {
        def rows
        if (objectName) {
            rows = sql.rows(INDEXES_COLUMNS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(INDEXES_COLUMNS_QUERY)
        }

        rows.each({

            def table = tables[it.table_name.toLowerCase()]
            def index = table.indexes[it.index_name.toLowerCase()]

            def indexColumn = new IndexColumn()
            indexColumn.name = it.column_name?.startsWith('SYS_NC') ? it.data_default?.trim() : it.column_name.toLowerCase()
            indexColumn.descend = it.descend.toLowerCase() == 'asc' ? false : true

            index.columns << indexColumn
        })
    }

    final static def CONSTRAINTS_QUERY = '''
        select uc.table_name, uc.constraint_name, uc.constraint_type, uc2.table_name ref_table,
           uc.index_name, uc.delete_rule, uc.status, uc.search_condition
        from   user_constraints uc
           left join user_constraints uc2 on uc.r_constraint_name = uc2.constraint_name
           join user_tables ut on uc.table_name = ut.table_name
           left join user_mviews v on uc.table_name = v.mview_name
        where  (uc.constraint_type <> 'C' or (uc.constraint_type = 'C' and uc.generated = 'USER NAME'))
          and mview_name is null
        order  by uc.table_name, uc.constraint_type, uc.constraint_name
	'''
    final static def CONSTRAINTS_QUERY_BY_NAME = '''
        select uc.table_name, uc.constraint_name, uc.constraint_type, uc2.table_name ref_table,
           uc.index_name, uc.delete_rule, uc.status, uc.search_condition
        from   user_constraints uc
           left join user_constraints uc2 on uc.r_constraint_name = uc2.constraint_name
           join user_tables ut on uc.table_name = ut.table_name
           left join user_mviews v on uc.table_name = v.mview_name
        where  (uc.constraint_type <> 'C' or (uc.constraint_type = 'C' and uc.generated = 'USER NAME'))
          and mview_name is null
		  and  uc.table_name = upper(?)
		order  by uc.table_name, uc.constraint_type, uc.constraint_name
	'''

    private def fillCostraints(tables, objectName) {
        def rows
        if (objectName) {
            rows = sql.rows(CONSTRAINTS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(CONSTRAINTS_QUERY)
        }

        rows.each({
            def tableName = it.table_name.toLowerCase()
            def table = tables[tableName]

            def constraint = new Constraint()
            constraint.name = it.constraint_name.toLowerCase()
            constraint.refTable = it.ref_table?.toLowerCase()
            constraint.type = it.constraint_type
            constraint.searchCondition = it.search_condition
            def onDelete = it.delete_rule?.toLowerCase()
            constraint.onDelete = onDelete == 'no action' ? null : onDelete
            def status = it.status?.toLowerCase()
            constraint.status = status
            table.constraints[constraint.name] = constraint
        })
    }

    final static def CONSTRAINTS_COLUMNS_QUERY = '''
        select ucc.table_name, ucc.constraint_name, ucc.column_name,
               case uc2.constraint_type when 'P' then '' else ucc2.column_name end ref_column_name
        from   user_cons_columns ucc
               join user_constraints uc on ucc.constraint_name = uc.constraint_name
               join user_tables ut on uc.table_name = ut.table_name
               left join user_constraints uc2 on uc.r_constraint_name = uc2.constraint_name
               left join user_cons_columns ucc2 on (uc2.constraint_name = ucc2.constraint_name and ucc.position = ucc2.position)
               left join user_mviews v on ucc.table_name = v.mview_name
        where  uc.constraint_type <> 'C'
          and  v.mview_name is null
        order  by ucc.table_name, ucc.constraint_name, ucc.position
	'''
    final static def CONSTRAINTS_COLUMNS_QUERY_BY_NAME = '''
        select ucc.table_name, ucc.constraint_name, ucc.column_name,
               case uc2.constraint_type when 'P' then '' else ucc2.column_name end ref_column_name
        from   user_cons_columns ucc
               join user_constraints uc on ucc.constraint_name = uc.constraint_name
               join user_tables ut on uc.table_name = ut.table_name
               left join user_constraints uc2 on uc.r_constraint_name = uc2.constraint_name
               left join user_cons_columns ucc2 on (uc2.constraint_name = ucc2.constraint_name and ucc.position = ucc2.position)
               left join user_mviews v on ucc.table_name = v.mview_name
        where  uc.constraint_type <> 'C'
          and  v.mview_name is null
		  and  uc.table_name = upper(?)
		order  by ucc.table_name, ucc.constraint_name, ucc.position
	'''

    private def fillCostraintsColumns(tables, objectName) {
        def rows
        if (objectName) {
            rows = sql.rows(CONSTRAINTS_COLUMNS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(CONSTRAINTS_COLUMNS_QUERY)
        }
        rows.each({
            def tableName = it.table_name.toLowerCase()
            def table = tables[tableName]
            def constraint = table.constraints[it.constraint_name.toLowerCase()]
            constraint.columns << it.column_name.toLowerCase()
            if (it.ref_column_name) {
                constraint.refColumns << it.ref_column_name.toLowerCase()
            }
        })
    }

    final static def SEQUENCES_QUERY = '''
			select us.sequence_name, us.min_value
			from   user_sequences us
			order  by sequence_name
		'''
    final static def SEQUENCES_QUERY_BY_NAME = '''
			select us.sequence_name, us.min_value
			from   user_sequences us
			where  sequence_name = upper(?)
			order  by sequence_name
		'''

    def getSequences(objectName) {
        def sequences = [:]
        def rows
        if (objectName) {
            rows = sql.rows(SEQUENCES_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(SEQUENCES_QUERY)
        }
        rows.each({
            def sequence = new Sequence()
            sequence.name = it.sequence_name.toLowerCase()
            sequence.minValue = it.min_value
            sequences[sequence.name] = sequence
        })
        return sequences
    }

    final static def VIEWS_QUERY = '''
		select view_name, text
		from   user_views
		order  by view_name
	'''
    final static def VIEWS_QUERY_BY_NAME = '''
		select view_name, text
		from   user_views
		where  view_name = upper(?)
		order  by view_name
	'''
    final static def VIEW_DEPENDENCIES = '''
        select name, referenced_name
        from user_dependencies
        where type='VIEW'
        and referenced_type='VIEW'
	'''
    final static def VIEW_DEPENDENCIES_BY_NAME = '''
        select name, referenced_name
        from user_dependencies
        where type='VIEW'
        and referenced_type='VIEW'
		and name = upper(?)
	'''

    def getViews(objectName) {
        def views = new LinkedHashMap<String, View>()
        List<GroovyRowResult> rows
        Map<String, List<GroovyRowResult>> dependenciesByView
        if (objectName) {
            rows = sql.rows(VIEWS_QUERY_BY_NAME, [objectName])
            dependenciesByView = sql.rows(VIEW_DEPENDENCIES_BY_NAME, [objectName])
                    .groupBy { (it.name as String).toLowerCase() }
        } else {
            rows = sql.rows(VIEWS_QUERY)
            dependenciesByView = sql.rows(VIEW_DEPENDENCIES)
                    .groupBy { (it.name as String).toLowerCase() }
        }

        rows.each({
            def view = new View()
            view.name = it.view_name.toLowerCase()
            view.text_oracle = it.text
            view.dependencies = dependenciesByView[view.name as String]?.collect {
                (it.referenced_name as String).toLowerCase()
            } ?: []
            views[view.name] = view
        })
        return views
    }

    final static def PROCEDURES_NAME_QUERY = '''
		select distinct name
		from user_source
		where type in ('PROCEDURE', 'FUNCTION')
	'''
    final static def PROCEDURES_NAME_QUERY_BY_NAME = '''
		select distinct name
		from user_source
		where type in ('PROCEDURE', 'FUNCTION')
		  and name = upper(?)
	'''

    def getProcedures(objectName) {
        def procedures = [:]
        def rows
        if (objectName) {
            rows = sql.rows(PROCEDURES_NAME_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(PROCEDURES_NAME_QUERY)
        }

        rows.each({
            def procedure = new Procedure(name: it.name.toLowerCase())
            procedures[procedure.name] = procedure
        })

        getProceduresBody(procedures, objectName)

        return procedures
    }

    final static def PROCEDURES_BODY_QUERY = '''
		select name, text
		from user_source
		where type in ('PROCEDURE', 'FUNCTION')
		order by name, line
	'''
    final static def PROCEDURES_BODY_QUERY_BY_NAME = '''
		select name, text
		from user_source
		where type in ('PROCEDURE', 'FUNCTION')
		  and name = upper(?)
		order by name, line
	'''

    def getProceduresBody(procedures, objectName) {
        def body = ''
        def name
        def rows

        if (objectName) {
            rows = sql.rows(PROCEDURES_BODY_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(PROCEDURES_BODY_QUERY)
        }

        rows.each({
            if (it.name.toLowerCase() != name) {
                if (name) {
                    def procedure = procedures[name]
					if (procedure) {
						procedure.text_oracle = body
					}
                }
                name = it.name.toLowerCase()
                body = it.text
            }
            body += it.text
        })
        def procedure = procedures[name]
		if (procedure) {
			procedure.text_oracle = body
		}
    }

    final static def PACKAGES_QUERY = '''
		select name, type, text
		from user_source
		where type in ('PACKAGE', 'PACKAGE BODY')
		order by name, type, line
	'''
    final static def PACKAGES_QUERY_BY_NAME = '''
		select name, type, text
		from user_source
		where type in ('PACKAGE', 'PACKAGE BODY')
		  and name = upper(?)
		order by name, type, line
	'''

    def getPackages(objectName) {
        def packages = [:]
        def rows

        if (objectName) {
            rows = sql.rows(PACKAGES_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(PACKAGES_QUERY)
        }

        rows.each({
            def packageName = it.name.toLowerCase()
            def pack = packages[packageName] ?: new Package()
            pack.name = packageName
            if (it.type == 'PACKAGE') {
                pack.text += it.text
            } else {
                pack.body += it.text
            }
            packages[pack.name] = pack
        })

        return packages
    }

    final static def TRIGGERS_QUERY = '''
		select name, text
		from user_source
		where type in 'TRIGGER'
		order by name, line
	'''
    final static def TRIGGERS_QUERY_BY_NAME = '''
		select name, text
		from user_source
		where type in 'TRIGGER'
		and  name = upper(?)
		order by name, line
	'''

    def getTriggers(objectName) {
        def triggers = [:]
        def rows

        if (objectName) {
            rows = sql.rows(TRIGGERS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(TRIGGERS_QUERY)
        }

        rows.each({
            def triggerName = it.name.toLowerCase()
            def trigger = triggers[triggerName] ?: new Trigger(name: triggerName, text_oracle: '')
            trigger.text_oracle += it.text
            triggers[triggerName] = trigger
        })

        return triggers
    }

    final static def USER_TYPES = "SELECT name, text from user_source where type = 'TYPE' order by name, line"

    final static def USER_TYPES_BY_NAME = "SELECT name, text from user_source where type = 'TYPE' and name = upper(?) order by name, line"

    def getUserTypes(objectName) {
        def userTypes = [:]
        def rows

        if (objectName) {
            rows = sql.rows(USER_TYPES_BY_NAME, [objectName])
        } else {
            rows = sql.rows(USER_TYPES)
        }

        rows.each({
            def name = it.name.toLowerCase()
            def userType = userTypes[name] ?: new UserType()
            userType.name = name
            userType.text += it.text
            userTypes[name] = userType
        })

        return userTypes
    }

    def getMViews(objectName) {
        def mviews = fillMViews(objectName)
        fillMViewsIndexes(mviews, objectName)
        fillMViewsIndexColumns(mviews, objectName)
        return mviews
    }

    final static def MVIEW_QUERY = "select mview_name, query from user_mviews"
    final static def MVIEW_QUERY_BY_NAME = "select mview_name, query from user_mviews where mview_name = upper(?)"

    private def fillMViews(objectName) {
        def mviews = [:]
        def rows
        if (objectName) {
            rows = sql.rows(MVIEW_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(MVIEW_QUERY)
        }

        rows.each({
            def mview = new MView()
            mview.name = it.mview_name.toLowerCase()
            mview.text_oracle = it.query.replaceAll("\",", "\",\n")
            mviews[mview.name] = mview
        })
        return mviews
    }

    final static def MVIEW_INDEXES_QUERY = '''
        select ui.table_name, ui.index_name, ui.index_type, uniqueness
        from   user_indexes ui
               left join user_constraints uc on ui.index_name = uc.index_name
               join user_mviews uv on ui.table_name = uv.mview_name
        where  (uc.index_name is null or (uc.index_name is not null and uc.index_name != uc.constraint_name))
          and  index_type <> 'LOB'
        order  by table_name, index_name
	'''
    final static def MVIEW_INDEXES_QUERY_BY_NAME = '''
        select ui.table_name, ui.index_name, ui.index_type, uniqueness
        from   user_indexes ui
               left join user_constraints uc on ui.index_name = uc.index_name
               join user_mviews uv on ui.table_name = uv.mview_name
        where  (uc.index_name is null or (uc.index_name is not null and uc.index_name != uc.constraint_name))
          and  index_type <> 'LOB'
		  and  ui.table_name = upper(?)
		order  by table_name, index_name
	'''

    private def fillMViewsIndexes(tables, objectName) {
        def rows
        if (objectName) {
            rows = sql.rows(MVIEW_INDEXES_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(MVIEW_INDEXES_QUERY)
        }

        rows.each({
            def tableName = it.table_name.toLowerCase()
            def table = tables[tableName]
            def index = new MViewIndex()
            index.name = it.index_name.toLowerCase()
            index.type = it.index_type[0].toLowerCase()
            index.unique = it.uniqueness == 'UNIQUE' ? true : false
            table.indexes[index.name] = index
        })
    }

    final static def MVIEW_INDEXES_COLUMNS_QUERY = '''
        select uic.table_name, uic.index_name, uic.column_name column_name, utc.data_default,
               uic.descend
        from   user_ind_columns uic
               join user_indexes ui on ui.index_name = uic.index_name
               left join user_constraints uc on uic.index_name = uc.index_name
               join user_tab_cols utc on (uic.column_name = utc.column_name and uic.table_name = utc.table_name)
               join user_mviews uv on uic.table_name = uv.mview_name
        where  (uc.index_name is null or (uc.index_name is not null and uc.index_name != uc.constraint_name))
        order  by uic.index_name, uic.column_position
	'''
    final static def MVIEW_INDEXES_COLUMNS_QUERY_BY_NAME = '''
        select uic.table_name, uic.index_name, uic.column_name column_name, utc.data_default,
               uic.descend
        from   user_ind_columns uic
               join user_indexes ui on ui.index_name = uic.index_name
               left join user_constraints uc on uic.index_name = uc.index_name
               join user_tab_cols utc on (uic.column_name = utc.column_name and uic.table_name = utc.table_name)
               join user_mviews uv on uic.table_name = uv.mview_name
        where  (uc.index_name is null or (uc.index_name is not null and uc.index_name != uc.constraint_name))
		  and  ui.table_name = upper(?)
		order  by uic.index_name, uic.column_position
	'''

    private def fillMViewsIndexColumns(tables, objectName) {
        def rows
        if (objectName) {
            rows = sql.rows(MVIEW_INDEXES_COLUMNS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(MVIEW_INDEXES_COLUMNS_QUERY)
        }

        rows.each({

            def table = tables[it.table_name.toLowerCase()]
            def index = table.indexes[it.index_name.toLowerCase()]

            def indexColumn = new MViewIndexColumn()
            indexColumn.name = it.column_name?.startsWith('SYS_NC') ? it.data_default?.trim() : it.column_name.toLowerCase()
            indexColumn.descend = it.descend.toLowerCase() == 'asc' ? false : true

            index.columns << indexColumn
        })
    }

    def cleanupSchema(Schema schema) {
        schema.tables.each {
            it.value.indexes.each {
                it.value.where = null
            }
            it.value.constraints.each {
                it.value.onUpdate = null
            }
        }
    }
}
