package br.com.bluesoft.bee.util

public class VersionHelper {

    def static isNewerThan9_6(String version) {
        def is_newer = false
        def major_version = version.tokenize('.')[0].toInteger()
        def minor_version = version.tokenize('.')[1].toInteger()
        if (major_version == 9 && minor_version >= 6) {
            is_newer = true
        } else if (major_version > 9) {
            is_newer = true
        }
        return is_newer
    }

    static float getVersion(String version) {
        try {
            return Float.parseFloat(version)
        } catch(NumberFormatException|NullPointerException e) {
            return 0;
        }
    }

    static int getVersionMajor(String version) {
        return  (int)getVersion(version)
    }

}
