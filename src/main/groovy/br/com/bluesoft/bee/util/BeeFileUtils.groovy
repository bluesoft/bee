package br.com.bluesoft.bee.util

import br.com.bluesoft.bee.upgrade.BeeVersionModule

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

public class BeeFileUtils {

    def static downloadFile(remote, local) {
        def file = new FileOutputStream(local)
        def out = new BufferedOutputStream(file)
        def url = new URL(remote)

        InputStream is

        try {
            is = new BufferedInputStream(url.openStream())

            byte[] buffer = new byte[1024]
            long len = 0
            def c

            def size_remote = getFileSizeRemote(remote)
            def version = BeeVersionModule.getLatestVersion()

            while ((c = is.read(buffer)) != -1) {
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

    def static getFileSizeRemote(url) {
        try {
            URLConnection conn = new URL(url).openConnection()
            return conn.getContentLength()
        } catch (Exception e) {
            println "fatal: could not get file size"
            e.printStackTrace()
        }
    }

    def static createDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    def static removeDir(File dir) {
        String[] files = dir.list()

        if (dir.exists()) {
            for (String s : files) {
                File cur_file = new File(dir.getPath(), s)

                if (cur_file.isDirectory()) {
                    removeDir(cur_file)
                }

                cur_file.delete()
            }
        }

        dir.delete()
    }

    def static listBaseJars(def source) {
        if (!source.isDirectory()) {
            return []
        }

        def result = []
        source.eachFile { result << it.name.substring(0, it.name.lastIndexOf('-')) }

        return result
    }

    def static removeOldJars(File source, def baseNames) {
        if (!source.isDirectory()) {
            return false
        }

        Set itens = baseNames.toSet()
        def names = source.list()

        names.each {
            if (it.lastIndexOf('-') > 0 && it.substring(0, it.lastIndexOf('-')) in itens) {
                new File(source, it).delete()
            }
        }

        return true
    }

    def static copyFolder(File source, File destination) {
        if (source.isDirectory()) {
            String[] files = source.list()

            for (String file : files) {
                File src_file = new File(source, file)
                File dest_file = new File(destination, file)

                copyFolder(src_file, dest_file)
            }
        } else {
            if (destination.exists()) {
                destination.delete()
            }

            try {
                destination.getParentFile().mkdirs()
                destination << source.text
            } catch (Exception e) {
                println "fatal: could not copy folder"
                e.printStackTrace()
            }
        }
    }

    def static unzip(source, destination) {
        ZipInputStream zipInput
        ZipEntry entry

        byte[] buffer = new byte[2048]

        try {
            File dest = new File(destination)

            if (!dest.exists()) {
                dest.mkdirs()
            }

            zipInput = new ZipInputStream(new FileInputStream(source))

            entry = zipInput.getNextEntry()

            while (entry != null) {
                String entryName = entry.getName()
                File file = new File(destination + File.separator + entryName)

                if (entry.isDirectory()) {
                    File newDir = new File(file.getAbsolutePath());

                    if (!newDir.exists()) {
                        if (!newDir.mkdirs()) {
                            return false
                        }
                    }
                } else {
                    FileOutputStream out = new FileOutputStream(file)

                    int len;

                    while ((len = zipInput.read(buffer)) > 0) {
                        out.write(buffer, 0, len)
                    }

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

    def static fixPermissions(String inst_dir) {
        File file = new File(inst_dir + "/bin/bee")

        file.setExecutable(true)
    }
}
