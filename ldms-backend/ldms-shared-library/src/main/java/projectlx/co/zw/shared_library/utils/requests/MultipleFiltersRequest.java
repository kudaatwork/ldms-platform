package projectlx.co.zw.shared_library.utils.requests;

import java.io.Serializable;

public class MultipleFiltersRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private int page;
    private int size;
    private String searchValue;

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getSearchValue() { return searchValue; }
    public void setSearchValue(String searchValue) { this.searchValue = searchValue; }

    @Override
    public String toString() {
        return "MultipleFiltersRequest{page=" + page + ", size=" + size + ", searchValue='" + searchValue + "'}";
    }
}
