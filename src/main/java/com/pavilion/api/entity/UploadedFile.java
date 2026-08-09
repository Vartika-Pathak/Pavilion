package com.pavilion.api.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

// Uploaded photo bytes, stored in the database rather than on local disk — Render's disk is
// ephemeral and gets wiped on every redeploy/restart, but the database (Aiven MySQL in
// production) is persistent. Fine at this app's scale (a handful of maintenance-request photos,
// capped per request); a dedicated object store would be the right call if photo volume grew.
@Entity
@Table(name = "uploaded_files")
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    // Deliberately not @Lob: Hibernate maps that to JDBC's Blob API, which SQLite's JDBC driver
    // doesn't implement (getBlob() throws SQLFeatureNotSupportedException). LONGVARBINARY instead
    // maps to plain getBytes()/setBytes(), which both SQLite and MySQL support.
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private long size;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
