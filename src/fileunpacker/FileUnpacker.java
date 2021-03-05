package fileunpacker;

import java.io.*;
import java.util.HashMap;
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
        HashSet<String> fileList = GetFileList(txt);
        String[] zipList = GetZipList(input);
        HashMap<String, HashSet<String>> fileMap = FindFilesInZip(input, zipList, fileList);
        UnpackZip(fileMap, output);
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
            return;
        }
    }

    private static HashSet<String> GetFileList(String listFile) {
        HashSet<String> fileList = new HashSet<>();
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
        //Throw error if there are no files in the list (or the list is not a text file)
        if (fileList.size() == 0)
        {
            ReportError("A kicsomagolandó fájlok listája üres, vagy nem szöveges fájl. Minden egyes fájlt külön sorba kell írni, más adatot nem tartalmazhat a listafájl!");
        }
        return fileList;
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
        //If there are no zip files in folder, throw error
        if (files.length == 0)
        {
            ReportError("A bemeneti mappában egy zip fájl sem volt megtalálható.");
        }
        return files;
    }

    private static HashMap<String, HashSet<String>> FindFilesInZip(String input, String[] zipList, HashSet<String> fileList)
    {
        //Create HashMap to store files list for each zip
        HashMap <String, HashSet<String>> fileMap = new HashMap<>();
        //Set int to index of zip to search
        int zipIndex = 0;
        while (fileList.size() > 0 && zipIndex <= zipList.length - 1)
        {
            String inputFile = input + zipList[zipIndex];
            try {
                HashSet<String> filesInThisZip = new HashSet<>(); //Store found files here
                //Load ZIP file as input stream, set buffer
                FileInputStream fis = new FileInputStream(inputFile);
                ZipInputStream zis = new ZipInputStream(fis);
                //Load first file in zip
                ZipEntry ze = zis.getNextEntry();
                while (ze != null && fileList.size() > 0) //Find files until the zip ends (null entry) or all files are found
                {
                    String fileName = ze.getName();
                    if (fileList.contains(fileName)) {
                        filesInThisZip.add(fileName); //Add found file to "files in this zip" list
                        zis.closeEntry();
                        //Remove unpacked file from "files to extract" list
                        fileList.remove(fileName);
                    }
                    //Load next file in zip
                    ze = zis.getNextEntry();
                }
                //Close zip and file input stream if file is fully unpacked.
                zis.close();
                fis.close();
                //Add file list from this zip to global file list
                fileMap.put(inputFile, filesInThisZip);
                //Go to next zip
                zipIndex++;
            } catch (IOException e) {
                ReportError(e.getMessage());
            }
        }
        //If there are missing files, throw error
        if (fileList.size() > 0) {
            ReportError("A következő fájlok egyik zip fájlban sem voltak megtalálhatóak:" + fileList);
        }
        return fileMap; //return global file map
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
        System.out.println("A program futása során hiba történt, ezért a kicsomagolás nem történt meg. A hiba leírása a " + errorFile.getAbsolutePath() + " fájlban található.");
        System.out.println("Nyomj Enter gombot a kilépéshez.");
        Scanner scan = new Scanner(System.in);
        scan.nextLine();
        System.exit(0); //Stop program
    }

    private static void UnpackZip(HashMap<String, HashSet<String>> fileMap, String output) {
        //Set output directory 
        File outputDir = new File(output);
        for (String zipFile : fileMap.keySet()) //Go through all files in zip file map
            try {
                //Load ZIP file as input stream, set buffer
                FileInputStream fis = new FileInputStream(zipFile);
                ZipInputStream zis = new ZipInputStream(fis);
                byte[] buffer = new byte[1024];
                //Load first file in zip
                ZipEntry ze = zis.getNextEntry();
                while (ze != null && fileMap.get(zipFile).size() > 0) //Unpack needed files until the zip ends (null entry) or all files are found
                {
                    String currentFileInZip = ze.getName();
                    if (fileMap.get(zipFile).contains(currentFileInZip)) {
                        File newFile = new File(output + File.separator + currentFileInZip);
                        System.out.println(currentFileInZip + " kicsomagolása a " + zipFile + " csomagból.");
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
                        fileMap.get(zipFile).remove(currentFileInZip);
                    }
                    //Load next file
                    ze = zis.getNextEntry();
                }
                //Close zip and file input stream if file is fully unpacked.
                zis.close();
                fis.close();
            } catch (IOException e) {
                ReportError(e.getMessage());
            }
        }

    private static void ShowResult(HashSet<String> fileList) {
        System.out.println("A fájlok kicsomagolása sikeresen megtörtént.");
    }

}
