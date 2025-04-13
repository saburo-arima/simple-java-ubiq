# Ubiq Employee Data Encryption and Decryption Tool

This Java application provides secure encryption and decryption of employee data using the Ubiq Security platform. The application reads employee records from a CSV file, encrypts sensitive information, and writes the encrypted data to a new CSV file. It can also decrypt previously encrypted data.

## Features

- Multi-threaded encryption and decryption for improved performance
- Support for both Japanese and English CSV files
- Secure handling of employee data including tax ID numbers (MyNumber in Japan)
- JSON-based credential management
- CSV processing with proper handling of quotes and escape characters

## Prerequisites

- Java 21 or higher
- Gradle 7.0 or higher
- Ubiq Security credentials (access key, secret key, encryption key)

## Project Structure

```
src/main/
├── java/
│   └── com/
│       └── example/
│           └── ubiq/
│               ├── AppUbiqEncryptDecrypt.java  # Main application
│               ├── CredentialsConfig.java      # Credentials configuration
│               └── EmployeeRecord.java         # Data model
├── resources/
    ├── employee_data.csv               # Original employee data (Japanese)
    ├── employee_data_en.csv            # Original employee data (English)
    ├── credentials.json                # Actual credentials (not checked into Git)
    └── credentials.json.sample         # Sample credentials template
```

## Building the Application

Build the application using Gradle:

```bash
./gradlew build
```

## Running the Application

Run the application using Gradle:

```bash
./gradlew run
```

To use the English version of the CSV files, add the `en` argument:

```bash
./gradlew run --args="en"
```

## Input and Output Files

### Input Files

- `src/main/resources/employee_data.csv` - Japanese employee data
- `src/main/resources/employee_data_en.csv` - English employee data

### Output Files

- `src/main/resources/encrypted_employee_data.csv` - Encrypted employee data
- `src/main/resources/decrypted_employee_data.csv` - Decrypted employee data (for verification)

## Setting Up Credentials

1. Copy `credentials.json.sample` to `credentials.json`
2. Update the values in `credentials.json` with your actual Ubiq Security credentials:

```json
{
  "myNumberCredentials": {
    "accessKey": "your-access-key",
    "secretKey": "your-secret-key",
    "encryptionKey": "your-encryption-key",
    "datasetName": "mynumber"
  },
  "generalCredentials": {
    "accessKey": "your-access-key",
    "secretKey": "your-secret-key",
    "encryptionKey": "your-encryption-key",
    "datasetName": "general"
  }
}
```

## Implementation Details

- The application uses multi-threading to improve encryption and decryption performance
- Thread pool size is limited to 5 threads to prevent resource exhaustion
- Employee data is encoded using Base64 before encryption
- The CSV parser is configured to handle quoted values properly
- Different datasets are used for different types of information (general data vs. tax ID)

## Dependencies

- Ubiq Security Java SDK (v2.2.2)
- OpenCSV (v5.7.1)
- BouncyCastle (v1.76)
- Jackson Databind (v2.14.0) 