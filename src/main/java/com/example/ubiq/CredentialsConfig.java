package com.example.ubiq;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubiqsecurity.UbiqCredentials;
import com.ubiqsecurity.UbiqFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 資格情報の設定を管理するクラス
 */
public class CredentialsConfig {
    
    @JsonProperty("myNumberCredentials")
    private CredentialInfo myNumberCredentials;
    
    @JsonProperty("generalCredentials")
    private CredentialInfo generalCredentials;
    
    /**
     * 資格情報の詳細を保持する内部クラス
     */
    public static class CredentialInfo {
        @JsonProperty("accessKeyId")
        private String accessKeyId;
        
        @JsonProperty("secretSigningKey")
        private String secretSigningKey;
        
        @JsonProperty("secretCryptoAccessKey")
        private String secretCryptoAccessKey;
        
        @JsonProperty("datasetName")
        private String datasetName;
        
        // Getters and Setters
        public String getAccessKeyId() {
            return accessKeyId;
        }
        
        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }
        
        public String getSecretSigningKey() {
            return secretSigningKey;
        }
        
        public void setSecretSigningKey(String secretSigningKey) {
            this.secretSigningKey = secretSigningKey;
        }
        
        public String getSecretCryptoAccessKey() {
            return secretCryptoAccessKey;
        }
        
        public void setSecretCryptoAccessKey(String secretCryptoAccessKey) {
            this.secretCryptoAccessKey = secretCryptoAccessKey;
        }
        
        public String getDatasetName() {
            return datasetName;
        }
        
        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
        }
        
        /**
         * この資格情報からUbiqCredentialsを作成します
         * @return UbiqCredentials
         */
        public UbiqCredentials createUbiqCredentials() {
            return UbiqFactory.createCredentials(
                accessKeyId,
                secretSigningKey,
                secretCryptoAccessKey,
                null);
        }
    }
    
    // Getters and Setters
    public CredentialInfo getMyNumberCredentials() {
        return myNumberCredentials;
    }
    
    public void setMyNumberCredentials(CredentialInfo myNumberCredentials) {
        this.myNumberCredentials = myNumberCredentials;
    }
    
    public CredentialInfo getGeneralCredentials() {
        return generalCredentials;
    }
    
    public void setGeneralCredentials(CredentialInfo generalCredentials) {
        this.generalCredentials = generalCredentials;
    }
    
    /**
     * 資格情報ファイルからCredentialsConfigを読み込みます
     * @param filePath 資格情報ファイルのパス
     * @return CredentialsConfig
     * @throws IOException ファイル読み込みエラー
     */
    public static CredentialsConfig loadFromFile(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filePath), CredentialsConfig.class);
    }
    
    /**
     * リソースからCredentialsConfigを読み込みます
     * @param resourcePath リソースのパス
     * @return CredentialsConfig
     * @throws IOException リソース読み込みエラー
     */
    public static CredentialsConfig loadFromResource(String resourcePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = CredentialsConfig.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("リソースが見つかりません: " + resourcePath);
            }
            return mapper.readValue(inputStream, CredentialsConfig.class);
        }
    }
} 