package hive.TBLS.model;

import java.util.Date;

public class syncLog {
    private Integer syncId;

    private String path;

    private Boolean isdir;

    private Boolean issynchronized;

    private Date modTime;

    public Integer getSyncId() {
        return syncId;
    }

    public void setSyncId(Integer syncId) {
        this.syncId = syncId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path == null ? null : path.trim();
    }

    public Boolean getIsdir() {
        return isdir;
    }

    public void setIsdir(Boolean isdir) {
        this.isdir = isdir;
    }

    public Boolean getIssynchronized() {
        return issynchronized;
    }

    public void setIssynchronized(Boolean issynchronized) {
        this.issynchronized = issynchronized;
    }

    public Date getModTime() {
        return modTime;
    }

    public void setModTime(Date modTime) {
        this.modTime = modTime;
    }
}