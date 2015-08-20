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
package br.com.bluesoft.bee;

import br.com.bluesoft.bee.service.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

public class BeeUpgradeModule implements BeeWriter {
	private String TMPDIR = System.getProperty("java.io.tmpdir") + "/bee-upgrade"

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

		println "New version found: bee-" + getLatestVersion()

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

		createDir(TMPDIR)

		downloadLatestVersion()
		applyChanges()
		fixPermissions()

		removeDir(TMPDIR)

		println "Bee is now up-to-date!"

		System.exit 0
	}
	
	def isLatestVersion() {
		def version_current = Float.parseFloat(Bee.getVersion())
		def version_latest  = Float.parseFloat(getLatestVersion())
		
		return (version_latest <= version_current) ? true : false
	}
	
	def getLatestVersion() {
		String url_latest = "https://github.com/bluesoft/bee/releases/latest"

		try {
			URLConnection con = new URL(url_latest).openConnection()
			con.connect()
	
			InputStream is = con.getInputStream()
			String url_redirected = con.getURL()
			
			def url_split = url_redirected.split("/")
			def version   = url_split[url_split.size() - 1]
	
			return version

		} catch (Exception e) {
			println "fatal: error while downloading"
			e.printStackTrace()

		} finally {
			try {
				is.close()				
			} catch (IOException ioe) {
				ioe.printStackTrace()
			}
		}
	}

	def downloadLatestVersion() {
		def version = getLatestVersion()

		String url_download = 'https://github.com/bluesoft/bee/releases/download/' + version + '/bee-' + version + '.zip'
		String local_file   = TMPDIR + '/bee-' + version + '.zip'

		downloadFile(url_download, local_file)
	}

	def downloadFile(remote, local) {
		def file = new FileOutputStream(local)
		def out  = new BufferedOutputStream(file)
		def url  = new URL(remote)
		
		try {
			InputStream is = new BufferedInputStream(url.openStream()) 
	
			byte[] buffer = new byte[1024]
			long len = 0
			def c
	
			def size_remote = getFileSizeRemote(remote)
			def version     = getLatestVersion()
			
			while((c = is.read(buffer)) != -1) {
				len += c
	
				print "\rDownloading: bee-" + version + ".zip " + Math.round(len * 100 / size_remote) + "% [" + len + "/" + size_remote + "]"
				out.write(buffer, 0, c)
			}
			
			println ""

			return true
	
		} catch (Exception e) {
		    println "fatal: could not download file"
			e.printStackTrace()

		} finally {
			try {			
				out.flush()
				out.close()
				is.close()
			} catch (IOException ioe) {
				ioe.printStackTrace()
			}
		}
	}

	def getFileSize(file) {
		f = new File(file)
		return (f.exists() ? f.length() : 0)
	}
	
	def getFileSizeRemote(url) {
		try {
			URLConnection conn = new URL(url).openConnection()
			return conn.getContentLength()
		} catch (Exception e) {
			println "fatal: could not get file size"
			e.printStackTrace()
		}
	}
	
	def createDir(dir) {
		File d = new File(dir)

		if(!d.exists())
			d.mkdirs()
	}

	def removeDir(dir) {
		File d = new File(dir)

		String[] files = d.list()

		if(d.exists()) {
			for(String s: files) {
				File cur_file = new File(d.getPath(), s)
				
				if(cur_file.isDirectory()) removeDir(cur_file.getPath())

				cur_file.delete()
			}
		}

		d.delete()
	}

	def fixPermissions() {
		String app_path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
		String inst_dir = app_path.replaceAll("lib/bee-[0-9]+\\.[0-9]+\\.jar", "")
		
		File file = new File(inst_dir + "/bin/bee")
		
		file.setExecutable(true)
	}
	
	def applyChanges() {
		String app_path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
		String inst_dir = app_path.replaceAll("lib/bee-[0-9]+\\.[0-9]+\\.jar", "")

		def version  = getLatestVersion()
		def release  = TMPDIR + '/bee-' + version + '.zip'
		def zip_dest = TMPDIR

		println "Unarchiving ..."
		unzip(release, zip_dest)

		println "Applying changes ..."
		def source = TMPDIR + '/bee-' + version
		
		removeDir(inst_dir)
		createDir(inst_dir)

		copyFolder(new File(source), new File(inst_dir))
	}
	
	def copyFolder(File source, File destination) {
		if(source.isDirectory()) {
			String[] files = source.list()

			for(String file: files) {
				File src_file  = new File(source, file)
				File dest_file = new File(destination, file)

				copyFolder(src_file, dest_file)
			}
		} else {
			try {
				destination.getParentFile().mkdirs()

				InputStream  is  = new FileInputStream(source)
				OutputStream out = new FileOutputStream(destination)

				byte[] buffer = new byte[1024]
				
				int len
				while((len = is.read(buffer)) > 0)
					out.write(buffer, 0, len)

			} catch (Exception e) {
				println "fatal: could not copy folder"
				e.printStackTrace()

			} finally {
				try {
					is.close()
					out.close()
				} catch (IOException ioe) {
					ioe.printStackTrace()
				}
			}
		}
	}

	def unzip(source, destination) {
		byte[] buffer = new byte[2048]
		
		try {
			File dest = new File(destination)
	
			if(!dest.exists()) dest.mkdirs()

			ZipInputStream zipInput = new ZipInputStream(new FileInputStream(source))

			ZipEntry entry = zipInput.getNextEntry()
			
			while(entry != null) {
				String entryName = entry.getName()
				File file = new File(destination + File.separator + entryName)

				if(entry.isDirectory()) {
					File newDir = new File(file.getAbsolutePath());

					if(!newDir.exists()) {
						if(!newDir.mkdirs()) return false
					}

				} else {				
					FileOutputStream out = new FileOutputStream(file)
		
					int len;
					
					while((len = zipInput.read(buffer)) > 0)
						out.write(buffer, 0, len)
		
					out.close()
				}

				entry = zipInput.getNextEntry()
			}

			zipInput.closeEntry()
			zipInput.close()

		} catch (IOException e) {
			e.printStackTrace()
		}
	}

	void log(String msg) {
		println msg
	}
}