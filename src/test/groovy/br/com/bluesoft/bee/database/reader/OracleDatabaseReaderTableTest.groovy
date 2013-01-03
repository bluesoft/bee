package br.com.bluesoft.bee.database.reader;

import static org.junit.Assert.*
import static org.mockito.Mockito.*

import org.junit.Before
import org.junit.Test

public class OracleDatabaseReaderTableTest {

	def reader

	@Before
	void 'set up'(){
		def tableRows = [
			[table_name:'PESSOA', temporary:'N', comments: '#Core'],
			[table_name:'KEYS', temporary:'Y', comments:null]
		]
		def columns = createColumns()
		def indexes = createIndexes()
		def indexesColumns = createIndexesColumns()
		def constraints = createConstraints()
		def constraintsColumns = createConstraintsColumns()

		def sql = [ rows: { query ->
				switch (query) {
					case OracleDatabaseReader.TABLES_QUERY: return tableRows; break;
					case OracleDatabaseReader.TABLES_COLUMNS_QUERY: return columns; break;
					case OracleDatabaseReader.INDEXES_QUERY: return indexes; break;
					case OracleDatabaseReader.INDEXES_COLUMNS_QUERY: return indexesColumns; break;
					case OracleDatabaseReader.CONSTRAINTS_QUERY: return constraints; break;
					case OracleDatabaseReader.CONSTRAINTS_COLUMNS_QUERY: return constraintsColumns; break;
				}
			} ]

		reader = new OracleDatabaseReader(sql)
	}

	@Test
	void 'it should return one table object for each table in the database'() {
		def tables = reader.getTables()

		assertEquals 2, tables.size()
	}

	@Test
	void 'it should fill the table name'() {
		def tables = reader.getTables()
		assertEquals 'pessoa', tables['pessoa'].name
		assertEquals 'keys', tables['keys'].name
	}

	@Test
	void 'it should fill the table temporary attribute'() {
		def tables = reader.getTables()
		assertEquals false, tables['pessoa'].temporary
		assertEquals true, tables['keys'].temporary
	}

	@Test
	void 'it should fill the table comment'() {
		def tables = reader.getTables()

		assertEquals '#Core', tables['pessoa'].comment
		assertTrue tables['pessoa'].shouldImportTheData()

		assertNull tables['keys'].comment
		assertFalse tables['keys'].shouldImportTheData()
	}
	
	@Test
	void 'it should fill columns of the table according to the database metadata'() {
		def tables = reader.getTables()
		assertEquals 3, tables['pessoa'].columns.size()
	}

	@Test
	void 'it should convert varchar2 to varchar'() {
		def tables = reader.getTables()
		assertEquals 'varchar', tables['pessoa'].columns['nome_razao'].type
	}

	@Test
	void 'it should convert "null" to null in the defaultValue'() {
		def tables = reader.getTables()
		assertNull tables['pessoa'].columns['nome_razao'].defaultValue
	}

	@Test
	void 'it should set the nullability of a column'() {
		def tables = reader.getTables()
		assertTrue tables['pessoa'].columns['nome_razao'].nullable
		assertFalse tables['pessoa'].columns['pessoa_key'].nullable
	}

	@Test
	void 'it should set the data size and scale'() {
		def tables = reader.getTables()
		assertEquals 8, tables['pessoa'].columns['valor'].size
		assertEquals 3, tables['pessoa'].columns['valor'].scale
	}

	@Test
	void 'it should set the default value'() {
		def tables = reader.getTables()
		assertEquals "5", tables['pessoa'].columns['valor'].defaultValue
	}

	@Test
	void 'it should fill table indexes according to the database metadata'() {
		def tables = reader.getTables()
		assertEquals 3, tables['pessoa'].indexes.size()
	}

	@Test
	void 'it should set index name'() {
		def indexes = reader.getTables()['pessoa'].indexes
		assertEquals 'idx_pessoa_key', indexes['idx_pessoa_key'].name
		assertEquals 'idx_nome_razao_upper', indexes['idx_nome_razao_upper'].name
		assertEquals 'idx_valor', indexes['idx_valor'].name
	}

	@Test
	void 'it should set index uniqueness'() {
		def indexes = reader.getTables()['pessoa'].indexes
		assertTrue indexes['idx_pessoa_key'].unique
		assertFalse indexes['idx_nome_razao_upper'].unique
		assertFalse indexes['idx_valor'].unique
	}

	@Test
	void 'it should set index type'() {
		def indexes = reader.getTables()['pessoa'].indexes
		assertEquals 'b', indexes['idx_pessoa_key'].type
		assertEquals 'f', indexes['idx_nome_razao_upper'].type
		assertEquals 'n', indexes['idx_valor'].type
	}

	@Test
	void 'it should fill the index columns'() {
		def index = reader.getTables()['pessoa'].indexes['idx_valor']
		assertEquals 2, index.columns.size()
	}

	@Test
	void 'it should fill the index column name'() {
		def index = reader.getTables()['pessoa'].indexes['idx_valor']
		assertEquals 'pessoa_key', index.columns[0].name
		assertEquals 'valor', index.columns[1].name
	}

	@Test
	void 'it should fill the index column name with the default data when its a functional index'() {
		def index = reader.getTables()['pessoa'].indexes['idx_nome_razao_upper']
		assertEquals 'UPPER("NOME_RAZAO")', index.columns[0].name
	}

	@Test
	void 'it should fill the index column descend attribute'() {
		def index = reader.getTables()['pessoa'].indexes['idx_valor']
		assertFalse index.columns[0].descend
		assertTrue index.columns[1].descend
	}

	@Test
	void 'it should fill the table constraints'() {
		def constraints = reader.getTables()['pessoa'].constraints
		assertEquals 3, constraints.size()
	}

	@Test
	void 'it should fill the constraint name'() {
		def constraints = reader.getTables()['pessoa'].constraints
		assertEquals 'pk_pessoa', constraints['pk_pessoa'].name
		assertEquals 'fk_pessoa_tipo_pessoa', constraints['fk_pessoa_tipo_pessoa'].name
		assertEquals 'uk_pessoa_nome_razao', constraints['uk_pessoa_nome_razao'].name
	}

	@Test
	void 'it should fill the constraint ref table'() {
		def constraints = reader.getTables()['pessoa'].constraints
		assertNull constraints['pk_pessoa'].refTable
		assertEquals 'tipo_pessoa', constraints['fk_pessoa_tipo_pessoa'].refTable
		assertNull constraints['uk_pessoa_nome_razao'].refTable
	}

	@Test
	void 'it should fill the constraint type'() {
		def constraints = reader.getTables()['pessoa'].constraints
		assertEquals 'P', constraints['pk_pessoa'].type
		assertEquals 'R', constraints['fk_pessoa_tipo_pessoa'].type
		assertEquals 'U', constraints['uk_pessoa_nome_razao'].type
	}

	@Test
	void 'it should fill the constraint columns'() {
		def constraints = reader.getTables()['pessoa'].constraints
		assertEquals 1, constraints['pk_pessoa'].columns.size()
		assertEquals 1, constraints['fk_pessoa_tipo_pessoa'].columns.size()
		assertEquals 1, constraints['uk_pessoa_nome_razao'].columns.size()
	}

	@Test
	void 'it should fill the constraint columns names'() {
		def constraints = reader.getTables()['pessoa'].constraints
		assertEquals 'pessoa_key', constraints['pk_pessoa'].columns[0]
		assertEquals 'tipo_pessoa_key', constraints['fk_pessoa_tipo_pessoa'].columns[0]
		assertEquals 'nome_razao', constraints['uk_pessoa_nome_razao'].columns[0]
	}

	def createColumns() {

		def pessoaKey = [table_name:'PESSOA',
					column_name:'PESSOA_KEY',
					data_type:"NUMBER",
					data_size:6,
					data_scale:"0",
					nullable:"N",
					data_default:null]

		def nomeRazao = [table_name:'PESSOA',
					column_name:'NOME_RAZAO',
					data_type:"VARCHAR2",
					data_size:30,
					data_scale:"0",
					nullable:"Y",
					data_default:"null"]

		def valor = [table_name:'PESSOA',
					column_name:'VALOR',
					data_type:"NUMBER",
					data_size:8,
					data_scale:3,
					nullable:"N",
					data_default:"5"]

		return [pessoaKey, nomeRazao, valor]
	}

	def createIndexes() {

		def idx_valor = [table_name:'PESSOA',
					index_name:'IDX_VALOR',
					index_type:"NORMAL",
					uniqueness:'NONUNIQUE']

		def idx_upper_nome_razao = [table_name:'PESSOA',
					index_name:'IDX_NOME_RAZAO_UPPER',
					index_type:"FUNCTION-BASED NORMAL",
					uniqueness:'NONUNIQUE']

		def idx_nome_razao = [table_name:'PESSOA',
					index_name:'IDX_PESSOA_KEY',
					index_type:"BITMAP",
					uniqueness:'UNIQUE']

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
					column_name:'SYS_NC00064$',
					data_default:'UPPER("NOME_RAZAO")',
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
