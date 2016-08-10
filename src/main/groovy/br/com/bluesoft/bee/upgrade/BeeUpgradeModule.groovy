/*
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Bluesoft Consultoria em Informatica Ltda.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 */
package br.com.bluesoft.bee.upgrade

import br.com.bluesoft.bee.data.BeeDataGeneratorAction
import br.com.bluesoft.bee.data.BeeDataValidatorAction
import br.com.bluesoft.bee.service.*
import br.com.bluesoft.bee.util.BeeFileUtils

public class BeeUpgradeModule implements BeeWriter {
	static private String TMPDIR  = System.getProperty("java.io.tmpdir") + "/bee-upgrade"
	static private String VERSION = getLatestVersion()

	def usage() {
		println "usage: bee upgrade"
		println "Actions:"
		println "         upgrade - Upgrade current version of Bee to the latest version available"
	}

	def parseOptions(options) {
		def arguments = options.arguments
		if(arguments.size < 1) {
			usage()
			System.exit 0
		}

		def action = options.actionName

		def actionRunner = null
		switch(action) {
			case "generate":
				if(arguments.size < 2) {
					usage()
					System.exit 0
				}
				actionRunner = new BeeDataGeneratorAction(options: options, out: this)
				break
			case "validate":
				actionRunner = new BeeDataValidatorAction(options: options, out: this)
				break;
		}

		return actionRunner
	}

	def run(options) {
		println "Checking for updates ..."

		if (isLatestVersion()) {
			println "Already up-to-date."
			System.exit 0
		}

		println "New version found: bee-" + VERSION

		def input = ""
		if(options.actionName == "y") {
			println "Do you want to continue? [Y/n] y"
			input = "y"
		} else {
			input  = System.console().readLine("Do you want to continue? [Y/n] ")
		}

		def option = (input == null || input == "") ? "y" : input

		if(option.toLowerCase() != "y") {
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

		System.exit 0
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
		def version_latest  = Float.parseFloat(VERSION)

		return (version_latest <= version_current) ? true : false
	}

	def static downloadLatestVersion() {
		def version = VERSION

		String url_download = BeeVersionModule.getLatestDownloadURL()
		String local_file   = TMPDIR + '/bee-' + version + '.zip'

		BeeFileUtils.downloadFile(url_download, local_file)
	}

	def static applyChanges() {
		String app_path = getAppPath()
		String inst_dir = app_path.replaceAll("lib/bee-[0-9]+\\.[0-9]+\\.jar", "")

		def version  = VERSION
		def release  = TMPDIR + '/bee-' + version + '.zip'
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
		String app_path = BeeUpgradeModule.getProtectionDomain().getCodeSource().getLocation().getPath()
		String inst_dir = app_path.replaceAll("lib/bee-[0-9]+\\.[0-9]+\\.jar", "")

		return inst_dir
	}

	def static die(String msg) {
		println msg
		System.exit 1
	}

	void log(String msg) {
		println msg
	}
}