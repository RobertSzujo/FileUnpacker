package main.java.fileunpacker;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUnpacker {

    public static void main(String[] args) {
        checkArgsSyntax(args);
        File inputDir = new File (args[0]);
        File outputDir = new File (args[1]);
        File fileListTxt = new File (args[2]);
        validateLocations (inputDir, outputDir, fileListTxt);
        Set<String> filesToUnpack = getFileList(fileListTxt);
        String[] zipFileList = getZipList(inputDir);
        //Create new ZipHandler object
        ZipHandler zipHandler = new ZipHandler (inputDir, zipFileList, filesToUnpack, outputDir);
        Map<String, Set<String>> fileLocations = zipHandler.searchFilesInZips();
        zipHandler.unpackZipFiles(fileLocations);
        showSuccesfulResult();
    }

    private static void checkArgsSyntax(String[] args) {
        if (args.length < 3) {
            reportError("Túl kevés paraméter került megadásra! Helyes használat: fileunpacker <zip fájlok mappája> <kicsomagolási mappa> <fájlok listáját tartalmazó szövegfájl>");
        } else if (args.length > 3) {
            reportError("Túl sok paraméter került megadásra! Helyes használat: fileunpacker <zip fájlok mappája> <kicsomagolási mappa> <fájlok listáját tartalmazó szövegfájl>");
        }
    }

    private static void validateLocations(File inputDir, File outputDir, File fileListTxt) {
        if (!inputDir.exists() || !inputDir.canRead()) {
            reportError("A bemeneti zip fájlokat tartalmazó mappa nem létezik, vagy nem olvasható!");
        }
        if (!outputDir.exists()) {
            System.out.println("A kimeneti mappa nem létezik, ezért létrehozásra kerül!");
            if (!outputDir.mkdir()) {
                reportError("Nem sikerült a kimeneti mappa létrehozása!");
            }
        }
        if (!outputDir.canWrite()) {
            reportError("A kimeneti mappa nem írható!");
        }
        if (!fileListTxt.canRead() || !fileListTxt.exists()) {
            reportError("A fájlok listáját tartalmazó szövegfájl nem létezik, vagy nem olvasható!");
        }
    }

    private static Set<String> getFileList(File fileListTxt) {
        Set<String> filesToUnpack = new HashSet<>();
        //Start to read list of files
        try {
            Scanner scan = new Scanner(fileListTxt);
            while (scan.hasNextLine()) { //Read list of files until txt ends.
                filesToUnpack.add(scan.nextLine());
            }
        } catch (Exception e) { //Call report error method if something's wrong
            reportError(e.getMessage());
        }
        //Throw error if there are no files in the list (or the list is not a text file)
        if (filesToUnpack.size() == 0) {
            reportError("A kicsomagolandó fájlok listája üres, vagy nem szöveges fájl. Minden egyes fájlt külön sorba kell írni, más adatot nem tartalmazhat a listafájl!");
        }
        return filesToUnpack;
    }

    private static String[] getZipList(File inputDir) {
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

    public static void reportError(String errorText) {
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

    private static void showSuccesfulResult() {
        System.out.println("A fájlok kicsomagolása sikeresen megtörtént.");
    }

}

class ZipHandler {
    File inputDir;
    String[] zipFileList;
    Set<String> filesToUnpack;
    File outputDir;

    public ZipHandler(File inputDir, String[] zipFileList, Set<String> filesToUnpack, File outputDir) {
        this.inputDir = inputDir;
        this.zipFileList = zipFileList;
        this.filesToUnpack = filesToUnpack;
        this.outputDir = outputDir;

    }

    public Map<String, Set<String>> searchFilesInZips() {
        Map<String, Set<String>> fileLocations = new HashMap<>();
        //Set int to index of zip to search
        int zipIndex = 0;
        while (filesToUnpack.size() > 0 && zipIndex <= zipFileList.length - 1) {
            String currentZipFile = inputDir.getAbsolutePath() + File.separator + zipFileList[zipIndex];
            searchInOneZip(currentZipFile, fileLocations);
            //Go to next zip
            zipIndex++;
        }
        //If there are missing files, throw error
        if (filesToUnpack.size() > 0) {
            FileUnpacker.reportError("A következő fájlok egyik zip fájlban sem voltak megtalálhatóak:" + filesToUnpack);
        }
        return fileLocations;
    }

    private void searchInOneZip(String currentZipFile, Map<String, Set<String>> fileLocations) {
        try {
            Set<String> filesInThisZip = new HashSet<>(); //Store found files here
            //Load ZIP file as input stream, set buffer
            FileInputStream fileInputStream = new FileInputStream(currentZipFile);
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            //Load first file in zip
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            //Find files until the zip ends (null entry) or all files are found
            while (zipEntry != null && filesToUnpack.size() > 0) {
                searchSingleFileInZip(filesToUnpack, filesInThisZip, zipInputStream, zipEntry);
                //Load next file in zip
                zipEntry = zipInputStream.getNextEntry();
            }
            //Close zip and file input stream if file is fully unpacked.
            zipInputStream.close();
            fileInputStream.close();
            //Add file list from this zip to global file list
            fileLocations.put(currentZipFile, filesInThisZip);
        } catch (IOException e) {
            FileUnpacker.reportError(e.getMessage());
        }
    }

    private static void searchSingleFileInZip(Set<String> filesToUnpack, Set<String> filesInThisZip, ZipInputStream zipInputStream, ZipEntry zipEntry) throws IOException {
        String fileName = zipEntry.getName();
        if (filesToUnpack.contains(fileName)) {
            filesInThisZip.add(fileName); //Add found file to "files in this zip" list
            zipInputStream.closeEntry();
            //Remove unpacked file from "files to extract" list
            filesToUnpack.remove(fileName);
        }
    }

    public void unpackZipFiles(Map<String, Set<String>> fileLocations) {
        for (String currentZipFile : fileLocations.keySet()) //Go through all files in zip file map
            unpackOneZip(currentZipFile, fileLocations);
    }

    private void unpackOneZip(String currentZipFile, Map<String, Set<String>> fileLocations) {
        try {
            //Load ZIP file as input stream, set buffer
            FileInputStream fileInputStream = new FileInputStream(currentZipFile);
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            byte[] buffer = new byte[1024];
            //Load first file in zip
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null && fileLocations.get(currentZipFile).size() > 0) {
                //Unpack needed files from zip one by one
                unpackSingleFileFromZip(fileLocations, outputDir, currentZipFile, zipInputStream, buffer, zipEntry);
                //Load next file
                zipEntry = zipInputStream.getNextEntry();
            }
            //Close zip and file input stream if file is fully unpacked.
            zipInputStream.close();
            fileInputStream.close();
        } catch (IOException e) {
            FileUnpacker.reportError(e.getMessage());
        }
    }

    private static void unpackSingleFileFromZip(Map<String, Set<String>> fileLocations, File outputDir, String currentZipFile, ZipInputStream zipInputStream, byte[] buffer, ZipEntry zipEntry) throws IOException {
        String currentFileInZip = zipEntry.getName();
        if (fileLocations.get(currentZipFile).contains(currentFileInZip)) {
            File newFile = new File(outputDir.getAbsolutePath() + File.separator + currentFileInZip);
            System.out.println(currentFileInZip + " kicsomagolása a " + currentZipFile + " csomagból.");
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
            fileLocations.get(currentZipFile).remove(currentFileInZip);
        }
    }
}
