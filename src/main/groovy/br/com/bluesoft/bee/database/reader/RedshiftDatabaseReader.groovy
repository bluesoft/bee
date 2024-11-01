package br.com.bluesoft.bee.database.reader

import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.TableColumn
import br.com.bluesoft.bee.model.View

class RedshiftDatabaseReader implements  DatabaseReader {

    def sql

    RedshiftDatabaseReader(def sql) {
        this.sql = sql
    }

    @Override
    Schema getSchema(objectName = null) {
        def schema = new Schema()
        schema.databaseVersion = getDatabaseVersion()
        schema.tables = getTables(objectName, schema.databaseVersion)
        schema.views = getViews(objectName)
        return schema
    }

    @Override
    def cleanupSchema(Schema schema) {
        schema.userTypes.clear()
        schema.packages.clear()
        schema.tables = schema.tables.findAll { !it.value.temporary }
    }

    def getDatabaseVersion() {
        return this.sql.connection.metaData.databaseProductVersion;
    }

    def getTables(objectName, databaseVersion) {
        def tables = fillTables(objectName)
        fillColumns(tables, objectName)
        return tables
    }

    private def getColumnType(String oracleColumnType) {
        switch (oracleColumnType.toLowerCase()) {
            case "character varying":
                return "varchar"
            case "timestamp without time zone":
                return "timestamp"
            default:
                return oracleColumnType.toLowerCase()
        }
    }

    static final def TABLES_QUERY = ''' 
        select n.nspname as schemaname, c.relname as table_name, 
          case releffectivediststyle
            when 0 then 'even'
            when 1 then 'key distkey(' || max(case when distkey then "column" end) || ')'
            when 8 then 'all'
          end as dist_style
        from pg_namespace n
        join pg_class c on n.oid = c.relnamespace and c.relkind = 'r'
        join pg_class_info i on c.oid = i.reloid
        join pg_table_def d on n.nspname = d.schemaname and c.relname = d.tablename
        where schemaname not in ('pg_catalog', 'information_schema')
          and pg_table_is_visible(c.oid) 
        group by n.nspname, c.relname, releffectivediststyle
        order by n.nspname, c.relname
	'''
    static final def TABLES_QUERY_BY_NAME = '''
        select n.nspname as schemaname, c.relname as table_name, 
          case releffectivediststyle
            when 0 then 'even'
            when 1 then 'key distkey(' || max(case when distkey then "column" end) || ')'
            when 8 then 'all'
          end as dist_style
        from pg_namespace n
        join pg_class c on n.oid = c.relnamespace and c.relkind = 'r'
        join pg_class_info i on c.oid = i.reloid
        join pg_table_def d on n.nspname = d.schemaname and c.relname = d.tablename
        where schemaname not in ('pg_catalog', 'information_schema')
          and pg_table_is_visible(c.oid) 
          and tablename = ?
        group by n.nspname, c.relname, releffectivediststyle
        order by n.nspname, c.relname
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
            def comment = ''
            tables[name] = new Table(name: name, temporary: false, comment: comment, distStyle: it.dist_style)
        })
        return tables
    }

    static final def TABLES_COLUMNS_QUERY = '''
        select c.relname as table_name
             , a.attname as column_name
             , regexp_substr(format_type(atttypid, a.atttypmod), '[^(]*') data_type
             , nullif(coalesce(nullif(ltrim(regexp_substr(format_type(atttypid, a.atttypmod), '\\\\([0-9]*'), '('), ''), '0')::int, 0) data_size
             , nullif(coalesce(nullif(ltrim(rtrim(regexp_substr(format_type(atttypid, a.atttypmod), ',.*'), ')'), ','), ''), '0')::int, 0) data_scale
             , not a.attnotnull nullable
             , false is_generated
             , ad.adsrc::information_schema.character_data data_default
             , a.attsortkeyord
        from pg_namespace n
          join pg_class c on n.oid = c.relnamespace and c.relkind = 'r'
          join pg_attribute a on c.oid = a.attrelid
          left join pg_attrdef ad on a.attrelid = ad.adrelid and a.attnum = ad.adnum
        where a.attnum > 0 
          and not a.attisdropped 
          and pg_table_is_visible(c.oid) 
          and n.nspname not in ('pg_catalog')
        order by c.relname, a.attnum
    '''

    static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
        select c.relname as table_name
             , a.attname as column_name
             , regexp_substr(format_type(atttypid, a.atttypmod), '[^(]*') data_type
             , nullif(coalesce(nullif(ltrim(regexp_substr(format_type(atttypid, a.atttypmod), '\\\\([0-9]*'), '('), ''), '0')::int, 0) data_size
             , nullif(coalesce(nullif(ltrim(rtrim(regexp_substr(format_type(atttypid, a.atttypmod), ',.*'), ')'), ','), ''), '0')::int, 0) data_scale
             , not a.attnotnull nullable
             , false is_generated
             , ad.adsrc::information_schema.character_data data_default
             , a.attsortkeyord
        from pg_namespace n
          join pg_class c on n.oid = c.relnamespace and c.relkind = 'r'
          join pg_attribute a on c.oid = a.attrelid
          left join pg_attrdef ad on a.attrelid = ad.adrelid and a.attnum = ad.adnum
        where a.attnum > 0 
          and not a.attisdropped 
          and pg_table_is_visible(c.oid) 
          and n.nspname not in ('pg_catalog')
          and c.relname = ?
        order by c.relname, a.attnum
	'''

    private def fillColumns(tables, objectName) {
        def rows
        if (objectName) {
            rows = sql.rows(TABLES_COLUMNS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(TABLES_COLUMNS_QUERY)
        }
        rows.each({
            def table_name = it.table_name.toLowerCase()
            if(!tables[table_name]) {
                println(table_name)
            }
            def table = tables[it.table_name.toLowerCase()]
            def column = new TableColumn()
            column.name = it.column_name.toLowerCase()
            column.type = getColumnType(it.data_type)
            column.size = it.data_size
            column.scale = it.data_scale == null ? 0 : it.data_scale
            column.nullable = it.nullable
            column.virtual = it.is_generated
            column.sortKeyOrder = it.attsortkeyord
            def defaultValue = it.data_default
            if (defaultValue) {
                column.defaultValue = defaultValue?.trim()?.toUpperCase() == 'NULL' ? null : defaultValue?.trim()
            }
            table.columns[column.name] = column
        })
    }

    final static def VIEWS_QUERY = '''
        select table_name as view_name, view_definition as text 
        from information_schema.views
        where table_schema not in ('information_schema', 'pg_catalog')
		order by view_name
	'''
    final static def VIEWS_QUERY_BY_NAME = '''
        select table_name as view_name, view_definition as text 
        from information_schema.views
        where table_schema not in ('information_schema', 'pg_catalog')
          and table_name = lower(?)
	'''

    def getViews(objectName) {
        def views = [:]
        def rows
        if (objectName) {
            rows = sql.rows(VIEWS_QUERY_BY_NAME, [objectName])
        } else {
            rows = sql.rows(VIEWS_QUERY)
        }

        rows.each({
            def view = new View()
            view.name = it.view_name.toLowerCase()
            view.text_postgres = it.text
            views[view.name] = view
        })
        return views
    }

}
