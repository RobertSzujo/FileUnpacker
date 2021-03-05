package fileunpacker;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUnpacker {

    public static void main(String[] args) {
        checkArgs(args);
        String input = args[0];
        String output = args[1];
        String txt = args[2];
        Set<String> fileList = getFileList(txt);
        String[] zipList = getZipList(input);
        Map<String, Set<String>> fileMap = findFilesInZips(input, zipList, fileList);
        unpackZipFiles(fileMap, output);
        showResult();
    }

    private static void checkArgs(String[] args) {
        if (args.length < 3) {
            reportError("Túl kevés paraméter került megadásra! Helyes használat: fileunpacker <zip fájlok mappája> <kicsomagolási mappa> <fájlok listáját tartalmazó szövegfájl>");
        } else if (args.length > 3) {
            reportError("Túl sok paraméter került megadásra! Helyes használat: fileunpacker <zip fájlok mappája> <kicsomagolási mappa> <fájlok listáját tartalmazó szövegfájl>");
        }
    }

    private static Set<String> getFileList(String listFile) {
        Set<String> fileList = new HashSet<>();
        File listTxt = new File(listFile);
        //Start to read list of files
        try {
            Scanner scan = new Scanner(listTxt);
            while (scan.hasNextLine()) { //Read list of files until txt ends.
                fileList.add(scan.nextLine());
            }
        } catch (Exception e) { //Call report error method if something's wrong
            reportError(e.getMessage());
        }
        //Throw error if there are no files in the list (or the list is not a text file)
        if (fileList.size() == 0) {
            reportError("A kicsomagolandó fájlok listája üres, vagy nem szöveges fájl. Minden egyes fájlt külön sorba kell írni, más adatot nem tartalmazhat a listafájl!");
        }
        return fileList;
    }

    private static String[] getZipList(String input) {
        File inputDir = new File(input);
        //Create filter to find .zip files in folder
        FilenameFilter filter = (inputDir1, name) -> name.toLowerCase().endsWith(".zip");
        //Get and return string array with list of zip files in folder
        String[] files = inputDir.list(filter);
        //If there are no zip files in folder, throw error
        assert files != null;
        if (files.length == 0) {
            reportError("A bemeneti mappában egy zip fájl sem volt megtalálható.");
        }
        return files;
    }

    private static Map<String, Set<String>> findFilesInZips(String input, String[] zipList, Set<String> fileList) {
        //Create Map to store files list for each zip
        Map<String, Set<String>> fileMap = new HashMap<>();
        //Set int to index of zip to search
        int zipIndex = 0;
        while (fileList.size() > 0 && zipIndex <= zipList.length - 1) {
            String inputFile = input + zipList[zipIndex];
            try {
                Set<String> filesInThisZip = new HashSet<>(); //Store found files here
                //Load ZIP file as input stream, set buffer
                FileInputStream fileInputStream = new FileInputStream(inputFile);
                ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
                //Load first file in zip
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                //Find files until the zip ends (null entry) or all files are found
                while (zipEntry != null && fileList.size() > 0) {
                    findFilesInOneZip(fileList, filesInThisZip, zipInputStream, zipEntry);
                    //Load next file in zip
                    zipEntry = zipInputStream.getNextEntry();
                }
                //Close zip and file input stream if file is fully unpacked.
                zipInputStream.close();
                fileInputStream.close();
                //Add file list from this zip to global file list
                fileMap.put(inputFile, filesInThisZip);
                //Go to next zip
                zipIndex++;
            } catch (IOException e) {
                reportError(e.getMessage());
            }
        }
        //If there are missing files, throw error
        if (fileList.size() > 0) {
            reportError("A következő fájlok egyik zip fájlban sem voltak megtalálhatóak:" + fileList);
        }
        return fileMap; //return global file map
    }

    private static void findFilesInOneZip(Set<String> fileList, Set<String> filesInThisZip, ZipInputStream zipInputStream, ZipEntry zipEntry) throws IOException {
        String fileName = zipEntry.getName();
        if (fileList.contains(fileName)) {
            filesInThisZip.add(fileName); //Add found file to "files in this zip" list
            zipInputStream.closeEntry();
            //Remove unpacked file from "files to extract" list
            fileList.remove(fileName);
        }
    }

    private static void reportError(String errorText) {
        //Create error file
        File errorFile = new File("errorLog.txt");
        try {
            FileWriter fw = new FileWriter(errorFile);
            fw.write(errorText);
            fw.close();
        } catch (IOException e) {
            System.out.println("Hiba történt a hibafájl írása során: " + e.getMessage());
        }
        //Display error info
        System.out.println("A program futása során hiba történt, ezért a kicsomagolás nem történt meg. A hiba leírása a " + errorFile.getAbsolutePath() + " fájlban található.");
        System.out.println("Nyomj Enter gombot a kilépéshez.");
        Scanner scan = new Scanner(System.in);
        scan.nextLine();
        System.exit(0); //Stop program
    }

    private static void unpackZipFiles(Map<String, Set<String>> fileMap, String output) {
        for (String zipFile : fileMap.keySet()) //Go through all files in zip file map
            try {
                //Load ZIP file as input stream, set buffer
                FileInputStream fileInputStream = new FileInputStream(zipFile);
                ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
                byte[] buffer = new byte[1024];
                //Load first file in zip
                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null && fileMap.get(zipFile).size() > 0) {
                    //Unpack needed files from zip one by one
                    unpackOneZip(fileMap, output, zipFile, zipInputStream, buffer, zipEntry);
                    //Load next file
                    zipEntry = zipInputStream.getNextEntry();
                }
                //Close zip and file input stream if file is fully unpacked.
                zipInputStream.close();
                fileInputStream.close();
            } catch (IOException e) {
                reportError(e.getMessage());
            }
    }

    private static void unpackOneZip(Map<String, Set<String>> fileMap, String output, String zipFile, ZipInputStream zipInputStream, byte[] buffer, ZipEntry zipEntry) throws IOException {
        String currentFileInZip = zipEntry.getName();
        if (fileMap.get(zipFile).contains(currentFileInZip)) {
            File newFile = new File(output + File.separator + currentFileInZip);
            System.out.println(currentFileInZip + " kicsomagolása a " + zipFile + " csomagból.");
            //Create output file stream
            FileOutputStream fos = new FileOutputStream(newFile);
            int length;
            while ((length = zipInputStream.read(buffer)) > 0) { //Write file until it ends.
                fos.write(buffer, 0, length);
            }
            //Stop file output and zip entry
            fos.close();
            zipInputStream.closeEntry();
            //Remove unpacked file from "files to extract" list
            fileMap.get(zipFile).remove(currentFileInZip);
        }
    }

    private static void showResult() {
        System.out.println("A fájlok kicsomagolása sikeresen megtörtént.");
    }

}
