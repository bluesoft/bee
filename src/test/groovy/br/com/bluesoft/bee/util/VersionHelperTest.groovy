package br.com.bluesoft.bee.util

import org.junit.Test

class VersionHelperTest {

    @Test
    void 'versao 8_5 deve retornar false'() {
        assert VersionHelper.isNewerThan9_6('8.5') == false
    }

    @Test
    void 'versao 9_5 deve retornar false'() {
        assert VersionHelper.isNewerThan9_6('9.5') == false
    }

    @Test
    void 'versao 9_6 deve retornar true'() {
        assert VersionHelper.isNewerThan9_6('9.6') == true
    }

    @Test
    void 'versao 9_7 deve retornar true'() {
        assert VersionHelper.isNewerThan9_6('9.7') == true
    }

    @Test
    void 'versao 10_3 deve retornar true'() {
        assert VersionHelper.isNewerThan9_6('10.3') == true
    }
}
