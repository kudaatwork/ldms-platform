package projectlx.co.zw.shared_library.model;

public enum  RecordAction {

    EDIT("EDIT"),CREATE("CREATE"),LOGIN("LOGIN"),DELETE("DELETE");

    private String action;

    public String getAction() {
        return action;
    }

    RecordAction(String action) {
        this.action = action;
    }
}
