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

import org.bouncycastle.crypto.InvalidCipherTextException;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVParser;
import com.opencsv.exceptions.CsvValidationException;
import com.ubiqsecurity.UbiqCredentials;
import com.ubiqsecurity.UbiqFactory;
import com.ubiqsecurity.UbiqStructuredEncryptDecrypt;

public class ApplicationUbiqDecrypt {
    // ファイル名の接尾辞
    private static final String ORIGINAL_DATA_FILENAME = "employee_data";
    private static final String ENCRYPTED_DATA_FILENAME = "encrypted_employee_data";
    private static final String DECRYPTED_DATA_FILENAME = "decrypted_employee_data";
    
    // 資格情報ファイルパス
    private static final String CREDENTIALS_RESOURCE_PATH = "credentials.json";
    
    // 言語設定
    private static boolean isEnglish = false;
    
    // 資格情報
    private static UbiqCredentials myNumberCredentials;
    private static UbiqCredentials generalCredentials;
    private static String myNumberDatasetName;
    private static String generalDatasetName;

    public static void main(String[] args) throws IllegalStateException, InvalidCipherTextException {
        // コマンドライン引数の処理
        if (args.length > 0 && "en".equals(args[0])) {
            isEnglish = true;
            System.out.println("英語版ファイルを使用します。");
        }
        

        // 資格情報の初期化
        try {
            initializeCredentialsFromJson();
        } catch (IOException e) {
            System.err.println("資格情報の読み込みに失敗しました: " + e.getMessage());
            System.err.println("デフォルトの資格情報を使用します。");
            initializeDefaultCredentials();
        }

        // 1. 暗号化処理
        encryptEmployeeData();
        
        // 2. 復号処理
        decryptEmployeeData();
    }

    /**
     * JSONファイルから資格情報を初期化します
     * @throws IOException ファイル読み込みエラー
     */
    private static void initializeCredentialsFromJson() throws IOException {
        try {
            // リソースからJSONを読み込む
            System.out.println("資格情報ファイルを読み込んでいます: " + CREDENTIALS_RESOURCE_PATH);
            CredentialsConfig config = CredentialsConfig.loadFromResource(CREDENTIALS_RESOURCE_PATH);
            
            // 資格情報を設定
            myNumberCredentials = config.getMyNumberCredentials().createUbiqCredentials();
            generalCredentials = config.getGeneralCredentials().createUbiqCredentials();
            myNumberDatasetName = config.getMyNumberCredentials().getDatasetName();
            generalDatasetName = config.getGeneralCredentials().getDatasetName();
            
            System.out.println("資格情報の読み込みが完了しました。");
            System.out.println("マイナンバーデータセット名: " + myNumberDatasetName);
            System.out.println("一般データセット名: " + generalDatasetName);
        } catch (Exception e) {
            System.err.println("資格情報の読み込みに失敗しました: " + e.getMessage());
            throw e;
        }
    }

    /**
     * デフォルトの資格情報を初期化します（JSONファイルの読み込みに失敗した場合のフォールバック）
     */
    private static void initializeDefaultCredentials() {
        myNumberCredentials = UbiqFactory.createCredentials(
            "xg4zjDF4nd138aP0mPJqdoUC",
            "/5rtAvGn9aX22DZi4WKG80h7lqH1qm7eedV2leI0iNvm",
            "ycLP7YBE1hKjXkW4zMUeOt/FkyctvlBPFyuQlVdBudrh",
            null);

        generalCredentials = UbiqFactory.createCredentials(
            "7Uq96Qo5IYodCc26oqvm6Yyf",
            "KIftGroBJHXqHFMVMQBDeRfqWiK58e/fpT7GB8O+m3rj",
            "JrfmAQCmuDzNzTrkM+tA0+Viy8Pf6brrnbnounurqwzE",
            null);
        
        myNumberDatasetName = "mynumber";
        generalDatasetName = "general";
    }

    /**
     * CSVParserを作成する
     * @return 設定済みのCSVParser
     */
    private static CSVParser createCSVParser() {
        return new CSVParserBuilder()
            .withSeparator(',')       // カンマ区切り
            .withQuoteChar('"')       // クォート文字
            .withEscapeChar('\\')     // エスケープ文字をバックスラッシュに設定
            .build();
    }

    /**
     * ファイルパスを生成します
     * @param baseFileName ベースファイル名
     * @return 言語設定に応じたファイルパス
     */
    private static String getFilePath(String baseFileName) {
        String fileSuffix = isEnglish ? "_en" : "";
        return Paths.get("src", "main", "resources", baseFileName + fileSuffix + ".csv").toString();
    }

    /**
     * CSVヘッダーを取得します
     * @return 言語設定に応じたCSVヘッダー
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
     * 従業員データを暗号化します
     */
    private static void encryptEmployeeData() throws IllegalStateException, InvalidCipherTextException {
        List<EmployeeRecord> employeeRecords = readEmployeeData();
        if (employeeRecords.isEmpty()) {
            System.err.println("従業員データが読み込めませんでした。");
            return;
        }

        writeEncryptedData(employeeRecords);
    }

    /**
     * 従業員データを読み込みます
     * @return 読み込んだ従業員レコードのリスト
     */
    private static List<EmployeeRecord> readEmployeeData() {
        List<EmployeeRecord> employeeRecords = new ArrayList<>();
        String employeeCsvPath = getFilePath(ORIGINAL_DATA_FILENAME);
        
        try {
            // カスタマイズしたCSVParserを使用
            CSVParser parser = createCSVParser();
            
            try (CSVReader reader = new CSVReaderBuilder(new FileReader(employeeCsvPath))
                    .withCSVParser(parser)
                    .build()) {
                // ヘッダー行を読み込む
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
            System.err.println("CSVファイルの読み込み中にエラーが発生: " + e.getMessage());
        }
        
        return employeeRecords;
    }

    /**
     * CSV行から従業員レコードを作成します
     * @param line CSV行
     * @return 従業員レコード
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
     * 暗号化されたデータを書き込みます
     * @param employeeRecords 従業員レコードのリスト
     */
    private static void writeEncryptedData(List<EmployeeRecord> employeeRecords) throws IllegalStateException, InvalidCipherTextException {
        String encryptedCsvPath = getFilePath(ENCRYPTED_DATA_FILENAME);
        
        try {
            // CSVWriterBuilderを使用してWriterを作成
            ICSVWriter writer = new CSVWriterBuilder(new FileWriter(encryptedCsvPath))
                .withSeparator(',')
                .withQuoteChar('"')
                .withEscapeChar('\\')  // エスケープ文字をバックスラッシュに設定
                .build();
            
            try {
                // ヘッダー行を書き込む
                writer.writeNext(getHeader());
                
                for (EmployeeRecord record : employeeRecords) {
                    String[] encryptedRecord = encryptRecord(record);
                    writer.writeNext(encryptedRecord);
                }            
                System.out.println("暗号化完了: " + encryptedCsvPath + " が作成されました: " + new File(encryptedCsvPath).getAbsolutePath());
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("CSVファイルの書き込み中にエラーが発生: " + e.getMessage());
        }
    }

    /**
     * 従業員レコードを暗号化します
     * @param record 従業員レコード
     * @return 暗号化された従業員データの配列
     */
    private static String[] encryptRecord(EmployeeRecord record) throws IllegalStateException, InvalidCipherTextException {
        return new String[] {
            encrypt(record.getEmployeeId(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeKanjiName(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeKanaName(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeDivision(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeTitle(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeHireDate(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeBirthday(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeSex(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeTel(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeEmail(), generalDatasetName, generalCredentials),
            encrypt(record.getEmployeeMyNumber(), myNumberDatasetName, myNumberCredentials)
        };
    }

    /**
     * 暗号化されたデータを復号します
     */
    private static void decryptEmployeeData() throws IllegalStateException, InvalidCipherTextException {
        List<String[]> encryptedRecords = readEncryptedData();
        if (encryptedRecords.isEmpty()) {
            System.err.println("暗号化されたデータが読み込めませんでした。");
            return;
        }

        writeDecryptedData(encryptedRecords);
    }

    /**
     * 暗号化されたデータを読み込みます
     * @return 暗号化されたレコードのリスト
     */
    private static List<String[]> readEncryptedData() {
        List<String[]> encryptedRecords = new ArrayList<>();
        String encryptedCsvPath = getFilePath(ENCRYPTED_DATA_FILENAME);
        
        try {
            // カスタマイズしたCSVParserを使用
            CSVParser parser = createCSVParser();
            
            try (CSVReader reader = new CSVReaderBuilder(new FileReader(encryptedCsvPath))
                    .withCSVParser(parser)
                    .build()) {
                // ヘッダー行を読み込む
                reader.readNext();
                
                String[] line;
                while ((line = reader.readNext()) != null) {
                    encryptedRecords.add(line);
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("暗号化されたCSVファイルの読み込み中にエラーが発生: " + e.getMessage());
        }
        
        return encryptedRecords;
    }

    /**
     * 復号されたデータを書き込みます
     * @param encryptedRecords 暗号化されたレコードのリスト
     */
    private static void writeDecryptedData(List<String[]> encryptedRecords) throws IllegalStateException, InvalidCipherTextException {
        String decryptedCsvPath = getFilePath(DECRYPTED_DATA_FILENAME);
        
        try {
            // CSVWriterBuilderを使用してWriterを作成
            ICSVWriter writer = new CSVWriterBuilder(new FileWriter(decryptedCsvPath))
                .withSeparator(',')
                .withQuoteChar('"')
                .withEscapeChar('\\')  // エスケープ文字をバックスラッシュに設定
                .build();
            
            try {
                // ヘッダー行を書き込む
                writer.writeNext(getHeader());
                
                for (String[] encryptedRecord : encryptedRecords) {
                    if (encryptedRecord.length >= 11) {
                        String[] decryptedRecord = decryptRecord(encryptedRecord);
                        writer.writeNext(decryptedRecord);
                    }
                }            
                System.out.println("復号完了: " + decryptedCsvPath + " が作成されました: " + new File(decryptedCsvPath).getAbsolutePath());
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("復号されたCSVファイルの書き込み中にエラーが発生: " + e.getMessage());
        }
    }

    /**
     * 暗号化されたレコードを復号します
     * @param encryptedRecord 暗号化されたレコード
     * @return 復号されたレコード
     */
    private static String[] decryptRecord(String[] encryptedRecord) throws IllegalStateException, InvalidCipherTextException {
        return new String[] {
            decrypt(encryptedRecord[0], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[1], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[2], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[3], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[4], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[5], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[6], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[7], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[8], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[9], generalDatasetName, generalCredentials),
            decrypt(encryptedRecord[10], myNumberDatasetName, myNumberCredentials)
        };
    }

    /**
     * 文字列を暗号化します
     * @param plainText 平文
     * @param datasetName データセット名
     * @param credentials 資格情報
     * @return 暗号化された文字列
     */
    private static String encrypt(String plainText, String datasetName, UbiqCredentials credentials) throws IllegalStateException, InvalidCipherTextException {
        try (UbiqStructuredEncryptDecrypt ubiqEncryptDecrypt = new UbiqStructuredEncryptDecrypt(credentials)) {
            System.out.println("暗号化: plainText: " + plainText + " datasetName: " + datasetName);

            byte[] utf8Bytes = plainText.getBytes(StandardCharsets.UTF_8);
            String base64Encrypted = Base64.getEncoder().encodeToString(utf8Bytes);
            
            System.out.println("base64Encrypted: " + base64Encrypted);
            String cipherText = ubiqEncryptDecrypt.encrypt(datasetName, base64Encrypted, null);
            
            System.out.println("暗号化結果: " + cipherText);
            return cipherText;
        }
    }

    /**
     * 暗号化された文字列を復号します
     * @param cipherText 暗号文
     * @param datasetName データセット名
     * @param credentials 資格情報
     * @return 復号された文字列
     */
    private static String decrypt(String cipherText, String datasetName, UbiqCredentials credentials) throws IllegalStateException, InvalidCipherTextException {
        try (UbiqStructuredEncryptDecrypt ubiqEncryptDecrypt = new UbiqStructuredEncryptDecrypt(credentials)) {
            System.out.println("復号: cipherText: " + cipherText + " datasetName: " + datasetName);

            // 暗号文を復号化
            String decryptedBase64 = ubiqEncryptDecrypt.decrypt(datasetName, cipherText, null);
            
            // Base64デコード
            byte[] decodedBytes = Base64.getDecoder().decode(decryptedBase64);
            String plainText = new String(decodedBytes, StandardCharsets.UTF_8);
            
            System.out.println("復号結果: " + plainText);
            return plainText;
        }
    }
} 