package br.com.bluesoft.bee

import org.junit.Test

class BeeTest {

    @Test
    void deve_obter_versao_atual_do_bee() {
        def beeVersion = Bee.getVersion()
        assert "test" == beeVersion
    }

}
