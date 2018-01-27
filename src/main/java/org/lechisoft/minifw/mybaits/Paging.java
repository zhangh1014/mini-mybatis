package org.lechisoft.minifw.mybaits;

public class Paging {
    private Boolean enabled = false;

    private Integer gotoPage = 1;
    private Integer pageSize = 10;
    private Integer totalPage = 1;
    private Integer totalRecord = 1;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getGotoPage() {
        return gotoPage;
    }

    public void setGotoPage(Integer gotoPage) {

        this.gotoPage = gotoPage < 1 ? 1 : gotoPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize < 1 ? 1 : pageSize;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage < 1 ? 1 : totalPage;
    }

    public Integer getTotalRecord() {
        return totalRecord;
    }

    public void setTotalRecord(Integer totalRecord) {
        this.totalRecord = totalRecord;
    }
}
