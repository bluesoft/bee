package br.com.bluesoft.bee.upgrade

import br.com.bluesoft.bee.runner.ActionRunner
import br.com.bluesoft.bee.service.BeeWriter
import br.com.bluesoft.bee.util.BeeFileUtils

public class BeeUpgradeAction implements ActionRunner {

    static private String TMPDIR = System.getProperty("java.io.tmpdir") + "/bee-upgrade"
    static private String VERSION = getLatestVersion()

    BeeWriter out
    def options


    @Override
    boolean validateParameters() {
        return true
    }

    boolean run() {
        println "Checking for updates ..."

        if (isLatestVersion()) {
            println "Already up-to-date."
            System.exit 0
        }

        println "New version found: bee-" + VERSION

        def input = ""
        if (options.actionName == "y") {
            println "Do you want to continue? [Y/n] y"
            input = "y"
        } else {
            input = System.console().readLine("Do you want to continue? [Y/n] ")
        }

        def option = (input == null || input == "") ? "y" : input

        if (option.toLowerCase() != "y") {
            println "Abort."
            return false
        }

        File tmpdir = new File(TMPDIR)

        BeeFileUtils.createDir(tmpdir)

        downloadLatestVersion()
        applyChanges()

        BeeFileUtils.fixPermissions(getAppPath())
        BeeFileUtils.removeDir(tmpdir)

        println "Bee is now up-to-date!"

        return true
    }


    def static getLatestVersion() {
        def version_latest = BeeVersionModule.getLatestVersion()

        def m = version_latest ==~ /^([0-9]+)\.([0-9]+)$/
        assert m instanceof Boolean

        if (!m) {
            def msg = "Fatal error: latest release name does not match the name pattern: N.N, e.g. 15.12.\n"
            msg += "Please open an issue with this output: https://github.com/bluesoft/bee/issues\n"
            msg += "Exiting.\n"

            die(msg)
        }

        return version_latest
    }

    def static isLatestVersion() {
        def version_current = Float.parseFloat(BeeVersionModule.getCurrentVersion())
        def version_latest = Float.parseFloat(VERSION)

        return (version_latest <= version_current) ? true : false
    }

    def static downloadLatestVersion() {
        def version = VERSION

        String url_download = BeeVersionModule.getLatestDownloadURL()
        String local_file = TMPDIR + '/bee-' + version + '.zip'

        BeeFileUtils.downloadFile(url_download, local_file)
    }

    def static applyChanges() {
        String app_path = getAppPath()
        String inst_dir = app_path.replaceAll("lib/bee-[0-9]+\\.[0-9]+\\.jar", "")

        def version = VERSION
        def release = TMPDIR + '/bee-' + version + '.zip'
        def zip_dest = TMPDIR

        println "Unarchiving ..."
        BeeFileUtils.unzip(release, zip_dest)

        println "Applying changes ..."
        def source = TMPDIR + '/bee-' + version

        File dest = new File(inst_dir)
        File src = new File(source)

        BeeFileUtils.copyFolder(src, dest)
        BeeFileUtils.removeOldBees(dest)
    }

    def static getAppPath() {
        String app_path = BeeUpgradeModule.getProtectionDomain().getCodeSource().getLocation().
                getPath()
        String inst_dir = app_path.replaceAll("lib/bee-[0-9]+\\.[0-9]+\\.jar", "")

        return inst_dir
    }

    def static die(String msg) {
        println msg
        System.exit 1
    }

    void log(String msg) {
        out.log msg
    }
}
