package fileunpacker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Scanner;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUnpacker {

    public static void main(String[] args) {
        //TODO: Make possible to choose your own directory/files.
        String input = "C:\\Users\\Robi\\Desktop\\fileunpacker\\input\\";
        String txt = "C:\\Users\\Robi\\Desktop\\fileunpacker\\txt\\list.txt";
        String output = "C:\\Users\\Robi\\Desktop\\fileunpacker\\output\\";
        HashSet<String> fileList = new HashSet<>();
        GetFileList(txt, fileList);
        HashSet<String> zipList = new HashSet<>();
        String[] inputFiles = GetZipList(input);
        UnpackZip(input, output, fileList);
    }
    
    private static void GetFileList (String listFile, HashSet<String> fileList)
    {
        File listTxt = new File (listFile);
        //Start to read list of files
        try
        {
            Scanner scan = new Scanner(listTxt);
            while (scan.hasNextLine()) //Read list of files until txt ends.
            {
                fileList.add(scan.nextLine());
            }
        } catch (Exception e) { //Call report error method if something's wrong
            ReportError(e.getMessage());
        }
    }
    
    private static String[] GetZipList (String input)
    {
        File inputDir = new File (input);
        //Create filter to find .zip files in folder
        FilenameFilter filter = new FilenameFilter()
        {
            public boolean accept (File inputDir, String name)
            {
                return name.toLowerCase().endsWith(".zip");
            }
        };
        String[] files = inputDir.list(filter);
        return files;
    }
    
    private static void ReportError (String errorText)
    {
        System.out.println(errorText);
        System.exit(0); //Stop program
        //TODO: Print error on screen and write to new text file
    }
    
    private static void UnpackZip (String inputFile, String outputDir, HashSet<String> fileList)
    {
        //Set output directory 
        File output = new File (outputDir);
        //Set int to number of files to search
        int filesToSearch = fileList.size();
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
            ReportError(e.getMessage());
        }
        
        
    }

}
