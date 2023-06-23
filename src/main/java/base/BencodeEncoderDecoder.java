package base;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class BencodeEncoderDecoder {

    private static final int PIECE_SIZE = 1000; // Piece size in bytes

    public static void main(String[] args) {
        String filePath = "src/main/resources/test.mp4"; // Replace with the actual MP4 file path
        String encodedFilePath = "src/main/resources/path_to_encoded_file.benc"; // Replace with the desired encoded file path
        String decodedFilePath = "src/main/resources/path_to_decoded_file.mp4"; // Replace with the desired decoded file path

        try {
            // Bencoding and encrypting the file
            Path path = Paths.get(filePath);
            byte[] fileBytes = Files.readAllBytes(path);
            String encodedData = encodeBytes(fileBytes);
            List<String> encryptedPieces = encryptAndSplit(encodedData);

            // Writing encoded and encrypted pieces to files
            writeEncodedDataToFile(encodedFilePath, encodedData);

            // Reading and decrypting the files
           // List<String> encryptedPiecesFromFile = readEncodedDataFromFile(encodedFilePath);
           // String mergedEncodedData = mergeAndDecrypt(encryptedPiecesFromFile);
            byte[] decodedBytes = decodeBytes(encodedData);
            writeDecodedDataToFile(decodedFilePath, decodedBytes);

            System.out.println("File successfully encoded, split, encrypted, decrypted, and saved.");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
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

    private static List<String> encryptAndSplit(String data) throws NoSuchAlgorithmException {
        List<String> encryptedPieces = new ArrayList<>();
        int startIndex = 0;
        int endIndex;
        while (startIndex < data.length()) {
            endIndex = Math.min(startIndex + PIECE_SIZE, data.length());
            String piece = data.substring(startIndex, endIndex);
            String encryptedPiece = encryptSHA256(piece);
            encryptedPieces.add(encryptedPiece);
            startIndex += PIECE_SIZE;
        }
        return encryptedPieces;
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

    private static void writeEncodedDataToFile(String filePath, String encodedData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

                writer.write(encodedData);
                writer.newLine();
            }

    }

    private static List<String> readEncodedDataFromFile(String filePath) throws IOException {
        List<String> encodedData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                encodedData.add(line);
            }
        }
        return encodedData;
    }

    private static String mergeAndDecrypt(List<String> encryptedPieces) {
        StringBuilder mergedEncodedData = new StringBuilder();
        for (String encryptedPiece : encryptedPieces) {
            mergedEncodedData.append(encryptedPiece);
        }
        return mergedEncodedData.toString();
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

    private static void writeDecodedDataToFile(String filePath, byte[] decodedData) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(decodedData);
        }
    }
}
