package com.example.ubiq;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bouncycastle.crypto.InvalidCipherTextException;

import com.opencsv.CSVReader;
import com.opencsv.ICSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVParser;
import com.opencsv.exceptions.CsvValidationException;
import com.ubiqsecurity.UbiqCredentials;
import com.ubiqsecurity.UbiqFactory;
import com.ubiqsecurity.UbiqStructuredEncryptDecrypt;

public class AppUbiqEncryptDecrypt {
    // File name suffixes
    private static final String ORIGINAL_DATA_FILENAME = "employee_data";
    private static final String ENCRYPTED_DATA_FILENAME = "encrypted_employee_data";
    private static final String DECRYPTED_DATA_FILENAME = "decrypted_employee_data";
    
    // Credential file path
    private static final String CREDENTIALS_RESOURCE_PATH = "credentials.json";
    
    // Language settings
    private static boolean isEnglish = false;
    
    // Credentials
    private static UbiqCredentials myNumberCredentials;
    private static UbiqCredentials generalCredentials;
    private static String myNumberDatasetName;
    private static String generalDatasetName;

    public static void main(String[] args) throws IllegalStateException, InvalidCipherTextException {
        // Process command line arguments
        if (args.length > 0 && "en".equals(args[0])) {
            isEnglish = true;
            System.out.println("Using English version files.");
        }
        

        // Initialize credentials
        try {
            initializeCredentialsFromJson();
        } catch (IOException e) {
            System.err.println("Failed to load credentials: " + e.getMessage());
            System.err.println("Using default credentials.");

        }

        // 1. Encryption process
        encryptEmployeeData();
        
        // 2. Decryption process
        decryptEmployeeData();
    }

    /**
     * Initialize credentials from JSON file
     * @throws IOException File reading error
     */
    private static void initializeCredentialsFromJson() throws IOException {
        try {
            // Read JSON from resource
            System.out.println("Loading credentials file: " + CREDENTIALS_RESOURCE_PATH);
            CredentialsConfig config = CredentialsConfig.loadFromResource(CREDENTIALS_RESOURCE_PATH);
            
            // Set credentials
            myNumberCredentials = config.getMyNumberCredentials().createUbiqCredentials();
            generalCredentials = config.getGeneralCredentials().createUbiqCredentials();
            myNumberDatasetName = config.getMyNumberCredentials().getDatasetName();
            generalDatasetName = config.getGeneralCredentials().getDatasetName();
            
            System.out.println("Credentials loaded successfully.");
            System.out.println("Tax ID dataset name: " + myNumberDatasetName);
            System.out.println("General dataset name: " + generalDatasetName);
        } catch (Exception e) {
            System.err.println("Failed to load credentials: " + e.getMessage());
            throw e;
        }
    }



    /**
     * Create a CSVParser
     * @return Configured CSVParser
     */
    private static CSVParser createCSVParser() {
        return new CSVParserBuilder()
            .withSeparator(',')       // Comma separator
            .withQuoteChar('"')       // Quote character
            .withEscapeChar('\\')     // Escape character set to backslash
            .build();
    }

    /**
     * Generate file path
     * @param baseFileName Base file name
     * @return File path according to language setting
     */
    private static String getFilePath(String baseFileName) {
        String fileSuffix = isEnglish ? "_en" : "";
        return Paths.get("src", "main", "resources", baseFileName + fileSuffix + ".csv").toString();
    }

    /**
     * Get CSV header
     * @return CSV header according to language setting
     */
    private static String[] getHeader() {
        if (isEnglish) {
            return new String[]{
                "Employee ID", "Full Name", "Name (Phonetic)", "Department", "Position", 
                "Hire Date", "Birth Date", "Gender", "Phone Number", "Email", "Tax ID"
            };
        } else {
            return new String[]{
                "社員番号", "氏名", "氏名（カナ）", "部署", "役職", 
                "入社日", "生年月日", "性別", "電話番号", "メールアドレス", "マイナンバー"
            };
        }
    }

    /**
     * Encrypt employee data
     */
    private static void encryptEmployeeData() throws IllegalStateException, InvalidCipherTextException {
        List<EmployeeRecord> employeeRecords = readEmployeeData();
        if (employeeRecords.isEmpty()) {
            System.err.println("Could not load employee data.");
            return;
        }

        writeEncryptedData(employeeRecords);
    }

    /**
     * Read employee data
     * @return List of employee records
     */
    private static List<EmployeeRecord> readEmployeeData() {
        List<EmployeeRecord> employeeRecords = new ArrayList<>();
        String employeeCsvPath = getFilePath(ORIGINAL_DATA_FILENAME);
        
        try {
            // Use customized CSVParser
            CSVParser parser = createCSVParser();
            
            try (CSVReader reader = new CSVReaderBuilder(new FileReader(employeeCsvPath))
                    .withCSVParser(parser)
                    .build()) {
                // Read header row
                reader.readNext();
                
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length >= 11) {
                        EmployeeRecord record = createEmployeeRecordFromLine(line);
                        employeeRecords.add(record);
                    }
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Error while reading CSV file: " + e.getMessage());
        }
        
        return employeeRecords;
    }

    /**
     * Create employee record from CSV line
     * @param line CSV line
     * @return Employee record
     */
    private static EmployeeRecord createEmployeeRecordFromLine(String[] line) {
        EmployeeRecord record = new EmployeeRecord();
        record.setEmployeeId(line[0]);
        record.setEmployeeKanjiName(line[1]);
        record.setEmployeeKanaName(line[2]);
        record.setEmployeeDivision(line[3]);
        record.setEmployeeTitle(line[4]);
        record.setEmployeeHireDate(line[5]);
        record.setEmployeeBirthday(line[6]);
        record.setEmployeeSex(line[7]);
        record.setEmployeeTel(line[8]);
        record.setEmployeeEmail(line[9]);
        record.setEmployeeMyNumber(line[10]);
        return record;
    }

    /**
     * Write encrypted data to file
     * @param employeeRecords List of employee records
     */
    private static void writeEncryptedData(List<EmployeeRecord> employeeRecords) throws IllegalStateException, InvalidCipherTextException {
        String encryptedCsvPath = getFilePath(ENCRYPTED_DATA_FILENAME);
        
        try {
            // Create Writer using CSVWriterBuilder
            ICSVWriter writer = new CSVWriterBuilder(new FileWriter(encryptedCsvPath))
                .withSeparator(',')
                .withQuoteChar('"')
                .withEscapeChar('\\')  // Escape character set to backslash
                .build();
            
            try {
                // Write header row
                writer.writeNext(getHeader());
                
                for (EmployeeRecord record : employeeRecords) {
                    String[] encryptedRecord = encryptRecord(record);
                    writer.writeNext(encryptedRecord);
                }            
                System.out.println("Encryption completed: " + encryptedCsvPath + " has been created: " + new File(encryptedCsvPath).getAbsolutePath());
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Error while writing CSV file: " + e.getMessage());
        }
    }

    /**
     * Encrypt employee record (multi-threaded version)
     * @param record Employee record
     * @return Array of encrypted employee data
     */
    private static String[] encryptRecord(EmployeeRecord record) throws IllegalStateException, InvalidCipherTextException {
        // Create thread pool with maximum 10 threads
        ExecutorService executor = Executors.newFixedThreadPool(10);
        String[] result = new String[11];
        
        try {
            // Create encryption tasks for each field
            List<Future<EncryptionResult>> futures = new ArrayList<>();
            
            // Encrypt general information (indices 0-9)
            futures.add(executor.submit(new EncryptionTask(0, record.getEmployeeId(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(1, record.getEmployeeKanjiName(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(2, record.getEmployeeKanaName(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(3, record.getEmployeeDivision(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(4, record.getEmployeeTitle(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(5, record.getEmployeeHireDate(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(6, record.getEmployeeBirthday(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(7, record.getEmployeeSex(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(8, record.getEmployeeTel(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(9, record.getEmployeeEmail(), generalDatasetName, generalCredentials)));
            futures.add(executor.submit(new EncryptionTask(10, record.getEmployeeMyNumber(), generalDatasetName, generalCredentials)));
            
            // Collect all task results
            for (Future<EncryptionResult> future : futures) {
                try {
                    EncryptionResult encryptionResult = future.get();
                    result[encryptionResult.getIndex()] = encryptionResult.getCipherText();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof InvalidCipherTextException) {
                        throw (InvalidCipherTextException) e.getCause();
                    } else {
                        throw new RuntimeException("Error during encryption process", e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Encryption process was interrupted", e);
                }
            }
        } finally {
            // Shutdown thread pool
            executor.shutdown();
        }
        
        return result;
    }
    
    /**
     * Class representing an encryption task
     */
    private static class EncryptionTask implements Callable<EncryptionResult> {
        private final int index;
        private final String plainText;
        private final String datasetName;
        private final UbiqCredentials credentials;
        
        public EncryptionTask(int index, String plainText, String datasetName, UbiqCredentials credentials) {
            this.index = index;
            this.plainText = plainText;
            this.datasetName = datasetName;
            this.credentials = credentials;
        }
        
        @Override
        public EncryptionResult call() throws InvalidCipherTextException {
            String cipherText = encrypt(plainText, datasetName, credentials);
            return new EncryptionResult(index, cipherText);
        }
    }
    
    /**
     * Class representing an encryption result
     */
    private static class EncryptionResult {
        private final int index;
        private final String cipherText;
        
        public EncryptionResult(int index, String cipherText) {
            this.index = index;
            this.cipherText = cipherText;
        }
        
        public int getIndex() {
            return index;
        }
        
        public String getCipherText() {
            return cipherText;
        }
    }

    /**
     * Decrypt encrypted data
     */
    private static void decryptEmployeeData() throws IllegalStateException, InvalidCipherTextException {
        List<String[]> encryptedRecords = readEncryptedData();
        if (encryptedRecords.isEmpty()) {
            System.err.println("Could not load encrypted data.");
            return;
        }

        writeDecryptedData(encryptedRecords);
    }

    /**
     * Read encrypted data
     * @return List of encrypted records
     */
    private static List<String[]> readEncryptedData() {
        List<String[]> encryptedRecords = new ArrayList<>();
        String encryptedCsvPath = getFilePath(ENCRYPTED_DATA_FILENAME);
        
        try {
            // Use customized CSVParser
            CSVParser parser = createCSVParser();
            
            try (CSVReader reader = new CSVReaderBuilder(new FileReader(encryptedCsvPath))
                    .withCSVParser(parser)
                    .build()) {
                // Read header row
                reader.readNext();
                
                String[] line;
                while ((line = reader.readNext()) != null) {
                    encryptedRecords.add(line);
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Error while reading encrypted CSV file: " + e.getMessage());
        }
        
        return encryptedRecords;
    }

    /**
     * Write decrypted data to file
     * @param encryptedRecords List of encrypted records
     */
    private static void writeDecryptedData(List<String[]> encryptedRecords) throws IllegalStateException, InvalidCipherTextException {
        String decryptedCsvPath = getFilePath(DECRYPTED_DATA_FILENAME);
        
        try {
            // Create Writer using CSVWriterBuilder
            ICSVWriter writer = new CSVWriterBuilder(new FileWriter(decryptedCsvPath))
                .withSeparator(',')
                .withQuoteChar('"')
                .withEscapeChar('\\')  // Escape character set to backslash
                .build();
            
            try {
                // Write header row
                writer.writeNext(getHeader());
                
                for (String[] encryptedRecord : encryptedRecords) {
                    if (encryptedRecord.length >= 11) {
                        String[] decryptedRecord = decryptRecord(encryptedRecord);
                        writer.writeNext(decryptedRecord);
                    }
                }            
                System.out.println("Decryption completed: " + decryptedCsvPath + " has been created: " + new File(decryptedCsvPath).getAbsolutePath());
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Error while writing decrypted CSV file: " + e.getMessage());
        }
    }

    /**
     * Decrypt encrypted record (multi-threaded version)
     * @param encryptedRecord Encrypted record
     * @return Decrypted record
     */
    private static String[] decryptRecord(String[] encryptedRecord) throws IllegalStateException, InvalidCipherTextException {
        // Create thread pool with maximum 10 threads
        ExecutorService executor = Executors.newFixedThreadPool(10);
        String[] result = new String[11];
        
        try {
            // Create decryption tasks for each field
            List<Future<DecryptionResult>> futures = new ArrayList<>();
            
            // Decrypt general information (indices 0-10)
            for (int i = 0; i < 11; i++) {
                System.out.println("Creating decryption task for index: " + i + " with cipher text: " + encryptedRecord[i]);
                futures.add(executor.submit(new DecryptionTask(i, encryptedRecord[i], generalDatasetName, generalCredentials)));
            }
            
            // Collect all task results
            for (Future<DecryptionResult> future : futures) {
                try {
                    DecryptionResult decryptionResult = future.get();
                    result[decryptionResult.getIndex()] = decryptionResult.getPlainText();
                    System.out.println("Decryption completed for index: " + decryptionResult.getIndex() + 
                                      " with result: " + decryptionResult.getPlainText());
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof InvalidCipherTextException) {
                        throw (InvalidCipherTextException) e.getCause();
                    } else {
                        throw new RuntimeException("Error during decryption process", e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Decryption process was interrupted", e);
                }
            }
            
            System.out.println("Decryption of all fields completed successfully");
        } finally {
            // Shutdown thread pool
            executor.shutdown();
        }
        
        return result;
    }
    
    /**
     * Class representing a decryption task
     */
    private static class DecryptionTask implements Callable<DecryptionResult> {
        private final int index;
        private final String cipherText;
        private final String datasetName;
        private final UbiqCredentials credentials;
        
        public DecryptionTask(int index, String cipherText, String datasetName, UbiqCredentials credentials) {
            this.index = index;
            this.cipherText = cipherText;
            this.datasetName = datasetName;
            this.credentials = credentials;
        }
        
        @Override
        public DecryptionResult call() throws InvalidCipherTextException {
            String plainText = decrypt(cipherText, datasetName, credentials);
            return new DecryptionResult(index, plainText);
        }
    }
    
    /**
     * Class representing a decryption result
     */
    private static class DecryptionResult {
        private final int index;
        private final String plainText;
        
        public DecryptionResult(int index, String plainText) {
            this.index = index;
            this.plainText = plainText;
        }
        
        public int getIndex() {
            return index;
        }
        
        public String getPlainText() {
            return plainText;
        }
    }

    /**
     * Encrypt a string
     * @param plainText Plain text
     * @param datasetName Dataset name
     * @param credentials Credentials
     * @return Encrypted string
     */
    private static String encrypt(String plainText, String datasetName, UbiqCredentials credentials) throws IllegalStateException, InvalidCipherTextException {
        try (UbiqStructuredEncryptDecrypt ubiqEncryptDecrypt = new UbiqStructuredEncryptDecrypt(credentials)) {
            System.out.println("Encrypting: plainText: " + plainText + " datasetName: " + datasetName);

            byte[] utf8Bytes = plainText.getBytes(StandardCharsets.UTF_8);
            String base64Encrypted = Base64.getEncoder().encodeToString(utf8Bytes);
            
            System.out.println("base64Encrypted: " + base64Encrypted);
            String cipherText = ubiqEncryptDecrypt.encrypt(datasetName, base64Encrypted, null);
            
            System.out.println("Encryption result: " + cipherText);
            return cipherText;
        }
    }

    /**
     * Decrypt an encrypted string
     * @param cipherText Cipher text
     * @param datasetName Dataset name
     * @param credentials Credentials
     * @return Decrypted string
     */
    private static String decrypt(String cipherText, String datasetName, UbiqCredentials credentials) throws IllegalStateException, InvalidCipherTextException {
        try (UbiqStructuredEncryptDecrypt ubiqEncryptDecrypt = new UbiqStructuredEncryptDecrypt(credentials)) {
            System.out.println("Decrypting: cipherText: " + cipherText + " datasetName: " + datasetName);

            // Decrypt cipher text
            String decryptedBase64 = ubiqEncryptDecrypt.decrypt(datasetName, cipherText, null);
            
            // Base64 decode
            byte[] decodedBytes = Base64.getDecoder().decode(decryptedBase64);
            String plainText = new String(decodedBytes, StandardCharsets.UTF_8);
            
            System.out.println("Decryption result: " + plainText);
            return plainText;
        }
    }
} 
