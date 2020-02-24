package edu.isi.oba;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import static edu.isi.oba.Oba.logger;


public class ObaUtils {
    /**
     * Method used to copy the local files: styles, images, etc.
     *
     * @param resourceName
     *            Name of the resource
     * @param dest
     *            file where we should copy it.
     */
    public static void copyLocalResource(String resourceName, File dest) {
        try {
            copy(ObaUtils.class.getResourceAsStream(resourceName), dest);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception while copying " + resourceName + " - " + e.getMessage());
        }
    }

    public static void copy(InputStream is, File dest) throws Exception {
        OutputStream os = null;
        try {
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception while copying resource. " + e.getMessage());
            throw e;
        } finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

    /**
     * Code to unzip a file. Inspired from
     * http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/ Taken
     * from
     *
     * @param resourceName
     * @param outputFolder
     */
    public static void unZipIt(String resourceName, String outputFolder) {

        byte[] buffer = new byte[1024];

        try {
            ZipInputStream zis = new ZipInputStream(ObaUtils.class.getResourceAsStream(resourceName));
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);
                // System.out.println("file unzip : "+ newFile.getAbsoluteFile());
                if (ze.isDirectory()) {
                    String temp = newFile.getAbsolutePath();
                    new File(temp).mkdirs();
                } else {
                    String directory = newFile.getParent();
                    if (directory != null) {
                        File d = new File(directory);
                        if (!d.exists()) {
                            d.mkdirs();
                        }
                    }
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Exception while copying resource. " + ex.getMessage());
        }

    }


}