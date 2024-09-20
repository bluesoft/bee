package br.com.bluesoft.bee.database.reader

import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.Table
import br.com.bluesoft.bee.model.TableColumn

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
//        schema.sequences = getSequences(objectName)
//        schema.views = getViews(objectName)
//        schema.procedures = getProcedures(objectName)
//        schema.packages = getPackages(objectName)
//        schema.triggers = getTriggers(objectName)
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
//        fillColumns(tables, objectName)
//        fillIndexes(tables, objectName, databaseVersion)
//        fillCostraints(tables, objectName)
//        fillCostraintsColumns(tables, objectName)
        return tables
    }

    static final def TABLES_QUERY = ''' 
        select distinct tablename table_name, 'N' as temporary
        from pg_table_def
        where schemaname not in ('pg_catalog', 'information_schema')
        order by tablename
	'''
    static final def TABLES_QUERY_BY_NAME = '''
        select distinct tablename table_name, 'N' as temporary
        from pg_table_def
        where schemaname not in ('pg_catalog', 'information_schema')
          and tablename = ?
        order by tablename
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
            def comment = ''
            tables[name] = new Table(name: name, temporary: temporary, comment: comment)
        })
        return tables
    }

    static final def TABLES_COLUMNS_QUERY = '''
        select c.relname as table_name
             , a.attname as column_name
             , regexp_substr(format_type(atttypid, a.atttypmod), '[^(]*') data_type
             , coalesce(nullif(ltrim(regexp_substr(format_type(atttypid, a.atttypmod), '\\\\([0-9]*'), '('), ''), '0')::int data_size
             , coalesce(nullif(ltrim(rtrim(regexp_substr(format_type(atttypid, a.atttypmod), ',.*'), ')'), ','), ''), '0')::int data_scale
             , not a.attnotnull nullable
             , false is_generated
             , a.attisdistkey as "distkey"
             , a.attsortkeyord as "sortkey"
             , ad.adsrc::information_schema.character_data data_default
        from pg_namespace n
          join pg_class c on n.oid = c.relnamespace
          join pg_attribute a on c.oid = a.attrelid
          left join pg_attrdef ad on a.attrelid = ad.adrelid and a.attnum = ad.adnum
        where a.attnum > 0 
          and not a.attisdropped 
          and pg_table_is_visible(c.oid) 
          and (c.relkind = 'r'::"char" or c.relkind = 'v'::"char")
          and n.nspname not in ('pg_catalog')
        order by c.relname, a.attnum
    '''

    static final def TABLES_COLUMNS_QUERY_BY_NAME = '''
        select c.relname as table_name
             , a.attname as column_name
             , regexp_substr(format_type(atttypid, a.atttypmod), '[^(]*') data_type
             , coalesce(nullif(ltrim(regexp_substr(format_type(atttypid, a.atttypmod), '\\\\([0-9]*'), '('), ''), '0')::int data_size
             , coalesce(nullif(ltrim(rtrim(regexp_substr(format_type(atttypid, a.atttypmod), ',.*'), ')'), ','), ''), '0')::int data_scale
             , not a.attnotnull nullable
             , false is_generated
             , a.attisdistkey as "distkey"
             , a.attsortkeyord as "sortkey"
             , ad.adsrc::information_schema.character_data data_default
        from pg_namespace n
          join pg_class c on n.oid = c.relnamespace
          join pg_attribute a on c.oid = a.attrelid
          left join pg_attrdef ad on a.attrelid = ad.adrelid and a.attnum = ad.adnum
        where a.attnum > 0 
          and not a.attisdropped 
          and pg_table_is_visible(c.oid) 
          and (c.relkind = 'r'::"char" or c.relkind = 'v'::"char")
          and n.nspname not in ('pg_catalog')
          and c.rel_name = ?
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
            def table = tables[it.table_name.toLowerCase()]
            def column = new TableColumn()
            column.name = it.column_name.toLowerCase()
            column.type = getColumnType(it.data_type)
            column.size = it.data_size
            column.scale = it.data_scale == null ? 0 : it.data_scale
            column.nullable = it.nullable == 'NO' ? false : true
            column.virtual = it.is_generated == 'ALWAYS'
            def defaultValue = it.data_default
            if (defaultValue) {
                column.defaultValue = defaultValue?.trim()?.toUpperCase() == 'NULL' ? null : defaultValue?.trim()
            }
            table.columns[column.name] = column
        })
    }

}
