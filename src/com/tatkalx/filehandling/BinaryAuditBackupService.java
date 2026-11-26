package com.tatkalx.filehandling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Byte-stream backup utility using FileInputStream and FileOutputStream
 * to create binary backups of SQLite database or forensic logs and verify byte counts.
 */
public class BinaryAuditBackupService {

    public long createBinaryBackup(String sourcePath, String destinationPath) throws IOException {
        File src = new File(sourcePath);
        if (!src.exists()) {
            throw new IOException("Source file does not exist for binary backup: " + sourcePath);
        }
        File dest = new File(destinationPath);
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        long totalBytesCopied = 0L;
        byte[] buffer = new byte[4096];
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesCopied += bytesRead;
            }
        }
        return totalBytesCopied;
    }

    public boolean verifyByteIdentity(String firstPath, String secondPath) throws IOException {
        File f1 = new File(firstPath);
        File f2 = new File(secondPath);
        if (!f1.exists() || !f2.exists() || f1.length() != f2.length()) {
            return false;
        }
        try (FileInputStream in1 = new FileInputStream(f1);
             FileInputStream in2 = new FileInputStream(f2)) {
            int b1;
            while ((b1 = in1.read()) != -1) {
                if (b1 != in2.read()) {
                    return false;
                }
            }
            return in2.read() == -1;
        }
    }
}
