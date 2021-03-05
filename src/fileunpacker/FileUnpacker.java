package fileunpacker;

import java.io.*;
import java.util.Scanner;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUnpacker {

    public static void main(String[] args) {
        CheckArgs(args);
        String input = args[0];
        String output = args[1];
        String txt = args[2];
        HashSet<String> fileList = new HashSet<>();
        GetFileList(txt, fileList);
        String[] zipList = GetZipList(input);
        UnpackZip(input, zipList, output, fileList);
        ShowResult(fileList);
    }

    private static void CheckArgs(String[] args)
    {
        if (args.length < 3)
        {
            ReportError("Túl kevés paraméter került megadásra! Helyes használat: fileunpacker <zip fájlok mappája> <kicsomagolási mappa> <fájlok listáját tartalmazó szövegfájl>");
        }
        else if (args.length > 3)
        {
            ReportError("Túl sok paraméter került megadásra! Helyes használat: fileunpacker <zip fájlok mappája> <kicsomagolási mappa> <fájlok listáját tartalmazó szövegfájl>");
        }
        else
        {
            //TODO: check if file/folder path is valid
            return;
        }
    }

    private static void GetFileList(String listFile, HashSet<String> fileList) {
        File listTxt = new File(listFile);
        //Start to read list of files
        try {
            Scanner scan = new Scanner(listTxt);
            while (scan.hasNextLine()) //Read list of files until txt ends.
            {
                fileList.add(scan.nextLine());
            }
        } catch (Exception e) { //Call report error method if something's wrong
            ReportError(e.getMessage());
        }
    }

    private static String[] GetZipList(String input) {
        File inputDir = new File(input);
        //Create filter to find .zip files in folder
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File inputDir, String name) {
                return name.toLowerCase().endsWith(".zip");
            }
        };
        //Get and return string array with list of zip files in folder
        String[] files = inputDir.list(filter);
        return files;
    }

    private static void ReportError(String errorText) {
        //Create error file
        File errorFile = new File ("errorLog.txt");
        try {
            FileWriter fw = new FileWriter(errorFile);
            fw.write(errorText);
            fw.close();
        } catch (IOException e)
        {
            System.out.println("Hiba történt a hibafájl írása során: " + e.getMessage());
        }
        //Display error info
        System.out.println("A program futása során hiba történt. A hiba leírása a " + errorFile.getAbsolutePath() + " fájlban található.");
        System.out.println("Nyomj Enter gombot a kilépéshez.");
        Scanner scan = new Scanner(System.in);
        scan.nextLine();
        System.exit(0); //Stop program
    }

    private static void UnpackZip(String inputDir, String[] inputFiles, String outputDir, HashSet<String> fileList) {
        //Set output directory 
        File output = new File(outputDir);
        //Set int to index of zip to search
        int zipIndex = 0;
        while (fileList.size() > 0 && zipIndex <= inputFiles.length - 1) { //Keep searching until all files are found or all zips are done
            String inputFile = inputDir + inputFiles[zipIndex];
            try {
                //Load ZIP file as input stream, set buffer
                FileInputStream fis = new FileInputStream(inputFile);
                ZipInputStream zis = new ZipInputStream(fis);
                byte[] buffer = new byte[1024];
                //Load first file in zip
                ZipEntry ze = zis.getNextEntry();
                while (ze != null && fileList.size() > 0) //Unpack files until the zip ends (null entry) or all files are found
                {
                    String fileName = ze.getName();
                    if (fileList.contains(fileName)) {
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
                        //Remove unpacked file from "files to extract" list
                        fileList.remove(fileName);
                    }
                    //Load next file
                    ze = zis.getNextEntry();
                }
                //Close zip and file input stream if file is fully unpacked.
                zis.close();
                fis.close();
                zipIndex++;
            } catch (IOException e) {
                ReportError(e.getMessage());
            }
        }

    }

    private static void ShowResult(HashSet<String> fileList) {
        if (fileList.size() > 0) {
            ReportError("A következő fájlok egyik zip fájlban sem voltak megtalálhatóak, ezért ezeket nem sikerült kicsomagolni:" + fileList);
        }
        else
        {
            System.out.println("A fájlok kicsomagolása sikeresen megtörtént.");
        }
    }

}
