package com.example.ubiq;

public class EmployeeRecord {
    private String employeeId;
    private String employeeKanjiName;
    private String employeeKanaName;
    private String employeeDivision;
    private String employeeTitle;
    private String employeeHireDate;
    private String employeeBirthday;
    private String employeeSex;
    private String employeeTel;
    private String employeeEmail;
    private String employeeMyNumber;

    // デフォルトコンストラクタ
    public EmployeeRecord() {
    }

    // 引数付きコンストラクタ
    public EmployeeRecord(String employeeId, String employeeKanjiName, String employeeKanaName,
                          String employeeDivision, String employeeTitle, String employeeHireDate,
                          String employeeBirthday, String employeeSex, String employeeTel,
                          String employeeEmail, String employeeMyNumber) {
        this.employeeId = employeeId;
        this.employeeKanjiName = employeeKanjiName;
        this.employeeKanaName = employeeKanaName;
        this.employeeDivision = employeeDivision;
        this.employeeTitle = employeeTitle;
        this.employeeHireDate = employeeHireDate;
        this.employeeBirthday = employeeBirthday;
        this.employeeSex = employeeSex;
        this.employeeTel = employeeTel;
        this.employeeEmail = employeeEmail;
        this.employeeMyNumber = employeeMyNumber;
    }

    // Getter メソッド
    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeKanjiName() {
        return employeeKanjiName;
    }

    public String getEmployeeKanaName() {
        return employeeKanaName;
    }

    public String getEmployeeDivision() {
        return employeeDivision;
    }

    public String getEmployeeTitle() {
        return employeeTitle;
    }

    public String getEmployeeHireDate() {
        return employeeHireDate;
    }

    public String getEmployeeBirthday() {
        return employeeBirthday;
    }

    public String getEmployeeSex() {
        return employeeSex;
    }

    public String getEmployeeTel() {
        return employeeTel;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public String getEmployeeMyNumber() {
        return employeeMyNumber;
    }

    // Setter メソッド
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public void setEmployeeKanjiName(String employeeKanjiName) {
        this.employeeKanjiName = employeeKanjiName;
    }

    public void setEmployeeKanaName(String employeeKanaName) {
        this.employeeKanaName = employeeKanaName;
    }

    public void setEmployeeDivision(String employeeDivision) {
        this.employeeDivision = employeeDivision;
    }

    public void setEmployeeTitle(String employeeTitle) {
        this.employeeTitle = employeeTitle;
    }

    public void setEmployeeHireDate(String employeeHireDate) {
        this.employeeHireDate = employeeHireDate;
    }

    // setEmployeeStartDateはsetEmployeeHireDateと同じ機能なので、別名として実装
    public void setEmployeeStartDate(String employeeHireDate) {
        this.employeeHireDate = employeeHireDate;
    }

    public void setEmployeeBirthday(String employeeBirthday) {
        this.employeeBirthday = employeeBirthday;
    }

    public void setEmployeeSex(String employeeSex) {
        this.employeeSex = employeeSex;
    }

    public void setEmployeeTel(String employeeTel) {
        this.employeeTel = employeeTel;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public void setEmployeeMyNumber(String employeeMyNumber) {
        this.employeeMyNumber = employeeMyNumber;
    }

    @Override
    public String toString() {
        return "EmployeeRecord{" +
                "employeeId='" + employeeId + '\'' +
                ", employeeKanjiName='" + employeeKanjiName + '\'' +
                ", employeeKanaName='" + employeeKanaName + '\'' +
                ", employeeDivision='" + employeeDivision + '\'' +
                ", employeeTitle='" + employeeTitle + '\'' +
                ", employeeHireDate='" + employeeHireDate + '\'' +
                ", employeeBirthday='" + employeeBirthday + '\'' +
                ", employeeSex='" + employeeSex + '\'' +
                ", employeeTel='" + employeeTel + '\'' +
                ", employeeEmail='" + employeeEmail + '\'' +
                ", employeeMyNumber='" + employeeMyNumber + '\'' +
                '}';
    }
}
