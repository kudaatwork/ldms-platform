package projectlx.co.zw.shared_library.utils.constants;

public class Constants {
    public static final String USER = "Authorisation";
    public static final String PHONE_NUMBER_REGEX = "^\\s?((\\+[1-9]{1,4}[ \\-]*)|(\\([0-9]{2,3}\\)[ \\-]*)|([0-9]{2,4})[ \\-]*)*?[0-9]{3,4}?[ \\-]*[0-9]{3,4}?\\s?";
    public static final String EMAIL_REGEX = "^(.+)@(\\S+)$";
    public static final String ZIM_NUMBER_REGEX = "^263\\d{9,}$";
    public static final String NATIONAL_ID_REGEX = "^[0-9]{2}-[0-9]{6,7}[A-Z]{1}[0-9]{2}$";
    public static final String LOCALE_LANGUAGE_NARRATIVE = "Internationalisation language, supported ";
    public static final String LOCALE_LANGUAGE = "Language";
    public static final String DEFAULT_LOCALE = "en";
    public static final String USERNAME_REGEX = "^[a-zA-Z][a-zA-Z0-9._]{2,19}$|^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,20}$";
    public static final String INTERNATIONAL_PHONE_REGEX = "^\\+\\d{1,3}\\d{6,14}$";
    public static final String ZIM_PHONE_REGEX = "^(\\+263|0)(71|73|77|78)\\d{6}$";
    public static final String ZIM_NID_REGEX = "^\\d{2}-\\d{6}-[A-Z]\\d{2}$";
}
