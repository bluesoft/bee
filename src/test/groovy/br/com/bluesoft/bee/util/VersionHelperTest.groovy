package br.com.bluesoft.bee.util

import org.junit.Test

class VersionHelperTest {

    @Test
    void 'versao 8_5 deve retornar false'() {
        assert !VersionHelper.isNewerThan9_6('8.5')
    }

    @Test
    void 'versao 9_5 deve retornar false'() {
        assert !VersionHelper.isNewerThan9_6('9.5')
    }

    @Test
    void 'versao 9_6 deve retornar true'() {
        assert VersionHelper.isNewerThan9_6('9.6')
    }

    @Test
    void 'versao 9_7 deve retornar true'() {
        assert VersionHelper.isNewerThan9_6('9.7')
    }

    @Test
    void 'versao 10_3 deve retornar true'() {
        assert VersionHelper.isNewerThan9_6('10.3')
    }

    @Test
    void 'versao 9_5 com string do SO deve retornar false'() {
        assert !VersionHelper.isNewerThan9_6('9.5 (Debian 12.2-2.pgdg100+1)')
    }

    @Test
    void 'versao 9_6 com string do SO deve retornar true'() {
        assert VersionHelper.isNewerThan9_6('9.6  (Debian 12.2-2.pgdg100+1)')
    }
}
