package br.com.bluesoft.bee.database.reader

import static org.junit.Assert.*
import static org.mockito.Mockito.*

import org.junit.Before
import org.junit.Test

class MySqlDatabaseReaderTableTest {

	def reader

	@Before
	void setup() {

		def tableRows = [
			[table_name:'PESSOA', temporary:'N', comments:'#Core'],
			[table_name:'KEYS', temporary:'N', comments:null]
		]
		def columns = createColumns()
		def indexes = createIndexes()
		def indexesColumns = createIndexesColumns()
		def constraints = createConstraints()
		def constraintsColumns = createConstraintsColumns()

		def sql = [ rows: { query ->
				switch (query) {
					case MySqlDatabaseReader.TABLES_QUERY: return tableRows; break;
					case MySqlDatabaseReader.TABLES_COLUMNS_QUERY: return columns; break;
					case MySqlDatabaseReader.INDEXES_QUERY: return indexes; break;
					case MySqlDatabaseReader.INDEXES_COLUMNS_QUERY: return indexesColumns; break;
					case MySqlDatabaseReader.CONSTRAINTS_QUERY: return constraints; break;
					case MySqlDatabaseReader.CONSTRAINTS_COLUMNS_QUERY: return constraintsColumns; break;
				}
			} ]

		reader = new MySqlDatabaseReader(sql, 'test')
	}

	@Test
	void 'it should return one table object for each table in the database'() {
		def tables = reader.getTables()

		assertEquals 2, tables.size()
	}

	@Test
	void 'it should fill the table name'() {
		def tables = reader.getTables()
		assertEquals 'PESSOA', tables['PESSOA'].name
		assertEquals 'KEYS', tables['KEYS'].name
	}

	@Test
	void 'it should fill the table comment'() {
		def tables = reader.getTables()

		assertEquals '#Core', tables['PESSOA'].comment
		assertTrue tables['PESSOA'].shouldImportTheData()

		assertNull tables['KEYS'].comment
		assertFalse tables['KEYS'].shouldImportTheData()
	}

	@Test
	void 'it should fill columns of the table according to the database metadata'() {
		def tables = reader.getTables()
		assertEquals 3, tables['PESSOA'].columns.size()
	}

	@Test
	void 'it should convert "null" to null in the defaultValue'() {
		def tables = reader.getTables()
		assertNull tables['PESSOA'].columns['NOME_RAZAO'].defaultValue
	}

	@Test
	void 'it should set the nullability of a column'() {
		def tables = reader.getTables()
		assertTrue tables['PESSOA'].columns['NOME_RAZAO'].nullable
		assertFalse tables['PESSOA'].columns['PESSOA_KEY'].nullable
	}

	@Test
	void 'it should set the data size and scale'() {
		def tables = reader.getTables()
		assertEquals 8, tables['PESSOA'].columns['VALOR'].size
		assertEquals 3, tables['PESSOA'].columns['VALOR'].scale
	}

	@Test
	void 'it should set the default value'() {
		def tables = reader.getTables()
		assertEquals "5", tables['PESSOA'].columns['VALOR'].defaultValue
	}

	@Test
	void 'it should fill table indexes according to the database metadata'() {
		def tables = reader.getTables()
		assertEquals 3, tables['PESSOA'].indexes.size()
	}

	@Test
	void 'it should set index name'() {
		def indexes = reader.getTables()['PESSOA'].indexes
		assertEquals 'IDX_PESSOA_KEY', indexes['IDX_PESSOA_KEY'].name
		assertEquals 'IDX_NOME_RAZAO_UPPER', indexes['IDX_NOME_RAZAO_UPPER'].name
		assertEquals 'IDX_VALOR', indexes['IDX_VALOR'].name
	}

	@Test
	void 'it should set index uniqueness'() {
		def indexes = reader.getTables()['PESSOA'].indexes
		assertTrue indexes['IDX_PESSOA_KEY'].unique
		assertFalse indexes['IDX_NOME_RAZAO_UPPER'].unique
		assertFalse indexes['IDX_VALOR'].unique
	}

	@Test
	void 'it should set index type'() {
		def indexes = reader.getTables()['PESSOA'].indexes
		assertEquals 'B', indexes['IDX_PESSOA_KEY'].type
		assertEquals 'F', indexes['IDX_NOME_RAZAO_UPPER'].type
		assertEquals 'N', indexes['IDX_VALOR'].type
	}

	@Test
	void 'it should fill the index columns'() {
		def index = reader.getTables()['PESSOA'].indexes['IDX_VALOR']
		assertEquals 2, index.columns.size()
	}

	@Test
	void 'it should fill the index column name'() {
		def index = reader.getTables()['PESSOA'].indexes['IDX_VALOR']
		assertEquals 'PESSOA_KEY', index.columns[0].name
		assertEquals 'VALOR', index.columns[1].name
	}

	@Test
	void 'it should fill the index column descend attribute'() {
		def index = reader.getTables()['PESSOA'].indexes['IDX_VALOR']
		assertFalse index.columns[0].descend
		assertTrue index.columns[1].descend
	}

	@Test
	void 'it should fill the table constraints'() {
		def constraints = reader.getTables()['PESSOA'].constraints
		assertEquals 3, constraints.size()
	}

	@Test
	void 'it should fill the constraint name'() {
		def constraints = reader.getTables()['PESSOA'].constraints
		assertEquals 'PK_PESSOA', constraints['PK_PESSOA'].name
		assertEquals 'FK_PESSOA_TIPO_PESSOA', constraints['FK_PESSOA_TIPO_PESSOA'].name
		assertEquals 'UK_PESSOA_NOME_RAZAO', constraints['UK_PESSOA_NOME_RAZAO'].name
	}

	@Test
	void 'it should fill the constraint ref table'() {
		def constraints = reader.getTables()['PESSOA'].constraints
		assertNull constraints['PK_PESSOA'].refTable
		assertEquals 'TIPO_PESSOA', constraints['FK_PESSOA_TIPO_PESSOA'].refTable
		assertNull constraints['UK_PESSOA_NOME_RAZAO'].refTable
	}

	@Test
	void 'it should fill the constraint type'() {
		def constraints = reader.getTables()['PESSOA'].constraints
		assertEquals 'P', constraints['PK_PESSOA'].type
		assertEquals 'R', constraints['FK_PESSOA_TIPO_PESSOA'].type
		assertEquals 'U', constraints['UK_PESSOA_NOME_RAZAO'].type
	}

	@Test
	void 'it should fill the constraint columns'() {
		def constraints = reader.getTables()['PESSOA'].constraints
		assertEquals 1, constraints['PK_PESSOA'].columns.size()
		assertEquals 1, constraints['FK_PESSOA_TIPO_PESSOA'].columns.size()
		assertEquals 1, constraints['UK_PESSOA_NOME_RAZAO'].columns.size()
	}

	@Test
	void 'it should fill the constraint columns names'() {
		def constraints = reader.getTables()['PESSOA'].constraints
		assertEquals 'PESSOA_KEY', constraints['PK_PESSOA'].columns[0]
		assertEquals 'TIPO_PESSOA_KEY', constraints['FK_PESSOA_TIPO_PESSOA'].columns[0]
		assertEquals 'NOME_RAZAO', constraints['UK_PESSOA_NOME_RAZAO'].columns[0]
	}
	
	
	def createColumns() {

		def pessoaKey = [table_name:'PESSOA',
					column_name:'PESSOA_KEY',
					data_type:"NUMBER",
					data_size:6,
					data_scale:"0",
					nullable:"NO",
					data_default:null]

		def nomeRazao = [table_name:'PESSOA',
					column_name:'NOME_RAZAO',
					data_type:"VARCHAR2",
					data_size:30,
					data_scale:"0",
					nullable:"YES",
					data_default:"null"]

		def valor = [table_name:'PESSOA',
					column_name:'VALOR',
					data_type:"NUMBER",
					data_size:8,
					data_scale:3,
					nullable:"NO",
					data_default:"5"]

		return [pessoaKey, nomeRazao, valor]
	}

	def createIndexes() {

		def idx_valor = [table_name:'PESSOA',
					index_name:'IDX_VALOR',
					index_type:"NORMAL",
					uniqueness:'0']

		def idx_upper_nome_razao = [table_name:'PESSOA',
					index_name:'IDX_NOME_RAZAO_UPPER',
					index_type:"FUNCTION-BASED NORMAL",
					uniqueness:'0']

		def idx_nome_razao = [table_name:'PESSOA',
					index_name:'IDX_PESSOA_KEY',
					index_type:"BITMAP",
					uniqueness:'1']

		return [
			idx_valor,
			idx_upper_nome_razao,
			idx_nome_razao
		]
	}

	def createIndexesColumns() {

		def idx_valor_pessoa_key = [
					table_name: 'PESSOA',
					index_name: 'IDX_VALOR',
					column_name:'PESSOA_KEY',
					data_default:null,
					descend:"ASC"]

		def idx_valor_valor = [
					table_name: 'PESSOA',
					index_name: 'IDX_VALOR',
					column_name:'VALOR',
					data_default:null,
					descend:"DESC"]

		def idx_nome_razao = [
					table_name: 'PESSOA',
					index_name: 'IDX_NOME_RAZAO_UPPER',
					column_name:'NOME_RAZAO',
					data_default:'NOME_RAZAO',
					descend:"ASC"]

		return [
			idx_valor_pessoa_key,
			idx_valor_valor,
			idx_nome_razao
		]
	}
	def createConstraints() {

		def primary = [
					table_name: 'PESSOA',
					constraint_name: 'PK_PESSOA',
					constraint_type:'P',
					ref_table:null,
					index_name:"PK_PESSOA"]

		def foreign = [
					table_name: 'PESSOA',
					constraint_name: 'FK_PESSOA_TIPO_PESSOA',
					constraint_type:'R',
					ref_table:"TIPO_PESSOA",
					index_name:null]

		def unique = [
					table_name: 'PESSOA',
					constraint_name: 'UK_PESSOA_NOME_RAZAO',
					constraint_type:'U',
					ref_table:null,
					index_name:"UK_PESSOA_NOME_RAZAO"]

		return [primary, foreign, unique]
	}

	def createConstraintsColumns(){
		def primary = [
					table_name: 'PESSOA',
					constraint_name: 'PK_PESSOA',
					column_name:'PESSOA_KEY'
				]

		def foreign = [
					table_name: 'PESSOA',
					constraint_name: 'FK_PESSOA_TIPO_PESSOA',
					column_name:'TIPO_PESSOA_KEY'
				]

		def unique = [
					table_name: 'PESSOA',
					constraint_name: 'UK_PESSOA_NOME_RAZAO',
					column_name:'NOME_RAZAO'
				]
		return [primary, foreign, unique]
	}
}
