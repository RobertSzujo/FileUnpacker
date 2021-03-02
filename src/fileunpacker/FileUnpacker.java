package fileunpacker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUnpacker {

    public static void main(String[] args) {
        //TODO: Make possible to choose your own directory/files.
        String input = "C:\\Users\\Robi\\Desktop\\fileunpacker\\input\\1.zip";
        String txt = "C:\\Users\\Robi\\Desktop\\fileunpacker\\txt\\list.txt";
        String output = "C:\\Users\\Robi\\Desktop\\fileunpacker\\output\\";
        UnpackZip(input, output);
    }
    
    private static void UnpackZip (String inputFile, String outputDir)
    {
        //TODO: unpack only specified list of files
        
        //Set output directory 
        File output = new File (outputDir);
        
        try{
            //Load ZIP file as input stream, set buffer
            FileInputStream fis = new FileInputStream(inputFile);
            ZipInputStream zis = new ZipInputStream(fis);
            byte[] buffer = new byte[1024];
            //Load first file in zip
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) //Unpack files until the zip ends (null entry)
            {
                String fileName = ze.getName();
                File newFile = new File(outputDir + File.separator + fileName);
                System.out.println(fileName + " kicsomagolása a " + inputFile + " csomagból.");
                //Create output file stream
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) //Write file until it ends.
                {
                    fos.write(buffer, 0, len);
                }
                //Stop file output and zip entry
                fos.close();
                zis.closeEntry();
                //Load next file
                ze = zis.getNextEntry();
            }
            //Close ZIP entry, zip and file input stream if file is fully unpacked.
            zis.closeEntry();
            zis.close();
            fis.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        
        
    }

}
