package base;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileSplitterBencoder {

    private static final int PIECE_SIZE = 1000000; // Piece size in bytes

    public static void main(String[] args) {
        String filePath = "src/main/resources/test.mp4"; // Replace with the actual large file path
        String splitFolderPath = "src/main/resources/test/"; // Replace with the desired folder path for split files
        String encodedFolderPath = "src/main/resources/enc/"; // Replace with the desired folder path for encoded files
        String decodedFilePath = "src/main/resources/decoded.mp4"; // Replace with the desired path for the decoded file

        try {
            // Splitting the large file
            splitFile(filePath, splitFolderPath);

            // Bencoding and encrypting each split file
            List<String> encodedFiles = new ArrayList<>();
            File splitFolder = new File(splitFolderPath);
            File[] splitFiles = splitFolder.listFiles();
            if (splitFiles != null) {
                for (File splitFile : splitFiles) {
                    byte[] splitBytes = Files.readAllBytes(splitFile.toPath());
                    String encodedData = encodeBytes(splitBytes);
                    String encryptedData = encryptSHA256(encodedData);
                    encodedFiles.add(encryptedData);
                }
            }

            // Writing encoded files to folder
            writeEncodedFiles(encodedFolderPath, encodedFiles);

            // Reading and decrypting the files
            List<String> encryptedFiles = readEncodedFiles(encodedFolderPath);
            List<byte[]> decryptedFiles = new ArrayList<>();
            for (String encryptedFile : encryptedFiles) {
                String decryptedData = decryptSHA256(encryptedFile);
                byte[] decodedBytes = decodeBytes(decryptedData);
                decryptedFiles.add(decodedBytes);
            }

            // Merging the decrypted files
            mergeFiles(decryptedFiles, decodedFilePath);

            System.out.println("File successfully split, encoded, encrypted, decrypted, and saved.");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void splitFile(String filePath, String splitFolderPath) throws IOException {
        Path path = Paths.get(filePath);
        long fileSize = Files.size(path);
        int numPieces = (int) Math.ceil((double) fileSize / PIECE_SIZE);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[PIECE_SIZE];
            int bytesRead;
            int pieceIndex = 1;
            while ((bytesRead = bis.read(buffer)) != -1) {
                String pieceFileName = String.format("%s/piece%d.txt", splitFolderPath, pieceIndex);
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pieceFileName))) {
                    bos.write(buffer, 0, bytesRead);
                }
                pieceIndex++;
            }
        }
    }

    private static String encodeBytes(byte[] data) {
        StringBuilder encodedData = new StringBuilder();
        encodedData.append(data.length).append(":");
        for (byte b : data) {
            encodedData.append((char) b);
        }
        return encodedData.toString();
    }

    private static String encryptSHA256(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(data.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static void writeEncodedFiles(String folderPath, List<String> encodedFiles) throws IOException {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        int fileIndex = 1;
        for (String encodedFile : encodedFiles) {
            String filePath = String.format("%s/encoded%d", folderPath, fileIndex);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write(encodedFile);
            }
            fileIndex++;
        }
    }

    private static List<String> readEncodedFiles(String folderPath) throws IOException {
        List<String> encodedFiles = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line = reader.readLine();
                    encodedFiles.add(line);
                }
            }
        }
        return encodedFiles;
    }

    private static String decryptSHA256(String encryptedData) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = hexStringToByteArray(encryptedData);
        byte[] decodedHash = digest.digest(encodedHash);
        StringBuilder decryptedData = new StringBuilder();
        for (byte b : decodedHash) {
            decryptedData.append((char) b);
        }
        return decryptedData.toString();
    }

    private static byte[] decodeBytes(String encodedData) {
        int separatorIndex = encodedData.indexOf(":");
        int dataLength = Integer.parseInt(encodedData.substring(0, separatorIndex));
        String data = encodedData.substring(separatorIndex + 1);
        byte[] decodedBytes = new byte[dataLength];
        for (int i = 0; i < dataLength; i++) {
            decodedBytes[i] = (byte) data.charAt(i);
        }
        return decodedBytes;
    }

    private static void mergeFiles(List<byte[]> files, String mergedFilePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mergedFilePath))) {
            for (byte[] file : files) {
                bos.write(file);
            }
        }
    }

    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
