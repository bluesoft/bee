package br.com.bluesoft.bee.dbchange

import br.com.bluesoft.bee.service.BeeWriter
import org.junit.Test

import java.sql.SQLException

class SQLExecutorTest {

    def messages = []

    private def criaLogger() {
        def logger = [log: { msg -> messages << msg; }] as BeeWriter
        return logger
    }

    @Test
    void "deve executar uma instrucao sql e logar"() {
        def logger = criaLogger()
        def sql = [executeUpdate: { sql -> return 0 }]
        def executor = new SQLExecutor(sql: sql, logger: logger)

        def retorno = executor.execute("select 1 from dual")

        assert messages.size() == 2
        assert messages[0] == ("select 1 from dual")
        assert messages[1].startsWith("Time elapsed:")
        assert retorno == true
    }

    @Test
    void "deve executar varias instrucoes sql e logar execucoes"() {
        def logger = criaLogger()
        def sql = [executeUpdate: { sql -> return 0 }]
        def executor = new SQLExecutor(sql: sql, logger: logger)

        def retorno = executor.execute([
                "select 1 from dual",
                "select * from x",
                "select a from b"
        ])

        assert messages.size() == 6
        assert messages[5].startsWith("Time elapsed:")
        assert retorno == true
    }

    @Test
    void "deve logar as execucoes com erro"() {
        def logger = criaLogger()
        def sql = [executeUpdate: { throw new SQLException("Erro") }]
        def executor = new SQLExecutor(sql: sql, logger: logger)

        def retorno = executor.execute(["select 1 from dual"])

        assert messages.size() == 2
        assert messages[0] == "select 1 from dual"
        assert messages[1] == "!!!Error: Erro"
        assert retorno == false
    }

    @Test
    void "nao deve logar as instrucoes no modo quiet"() {
        def logger = criaLogger()
        def sql = [executeUpdate: { sql -> return 0 }]
        def executor = new SQLExecutor(sql: sql, logger: logger, quiet: true)

        def retorno = executor.execute(["select 1 from dual", "select * from x"])

        assert messages.isEmpty()
        assert retorno == true
    }

    @Test
    void "deve logar somente a instrucao que falhou no modo quiet"() {
        def logger = criaLogger()
        def executadas = []
        def sql = [executeUpdate: { sql ->
            executadas << sql
            if (sql.startsWith("alter")) {
                throw new SQLException("Erro")
            }
            return 0
        }]
        def executor = new SQLExecutor(sql: sql, logger: logger, quiet: true)

        def retorno = executor.execute(["select 1 from dual", "alter table x add y int", "select 2 from dual"])

        assert retorno == false
        assert executadas.size() == 2
        assert messages == ["alter table x add y int", "!!!Error: Erro"]
    }
}
