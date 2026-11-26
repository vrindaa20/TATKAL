package com.tatkalx.model;

/**
 * Represents an Administrator user aligned with the Admins SQLite table.
 */
public class Admin extends User {

    private int adminId;
    private String employeeCode;
    private String department;
    private int clearanceLevel;
    private String lastAuditAt;

    public Admin() {
        super();
        this.role = "ADMIN";
    }

    public Admin(int userId, int adminId, String username, String fullName,
                 String email, String phone, String employeeCode,
                 String department, int clearanceLevel) {
        super(userId, username, "", fullName, email, phone, "ADMIN", true, "");
        this.adminId = adminId;
        this.employeeCode = employeeCode;
        this.department = department;
        this.clearanceLevel = clearanceLevel;
    }

    @Override
    public String getRoleDescription() {
        return "Administrator [" + employeeCode + ", Clearance L" + clearanceLevel + "]";
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getClearanceLevel() {
        return clearanceLevel;
    }

    public void setClearanceLevel(int clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }

    public String getLastAuditAt() {
        return lastAuditAt;
    }

    public void setLastAuditAt(String lastAuditAt) {
        this.lastAuditAt = lastAuditAt;
    }
}
