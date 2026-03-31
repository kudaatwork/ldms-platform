package projectlx.co.zw.shared_library.utils.globalvalidators;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.enums.Gender;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.enums.StorageProvider;
import projectlx.co.zw.shared_library.utils.enums.VerificationMethod;
import projectlx.co.zw.shared_library.utils.enums.VerificationSource;
import projectlx.co.zw.shared_library.utils.enums.VerificationTargetType;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validators {

    private static final Set<String> COUNTRIES = Set.of(
            "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua and Barbuda", "Argentina", "Armenia", "Australia", "Austria",
            "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan",
            "Bolivia", "Bosnia and Herzegovina", "Botswana", "Brazil", "Brunei", "Bulgaria", "Burkina Faso", "Burundi", "Cabo Verde", "Cambodia",
            "Cameroon", "Canada", "Central African Republic", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo (Congo-Brazzaville)",
            "Congo (Democratic Republic)", "Costa Rica", "Croatia", "Cuba", "Cyprus", "Czech Republic", "Denmark", "Djibouti", "Dominica",
            "Dominican Republic", "Ecuador", "Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia", "Eswatini", "Ethiopia",
            "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada",
            "Guatemala", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Honduras", "Hungary", "Iceland", "India", "Indonesia",
            "Iran", "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya",
            "Kiribati", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein",
            "Lithuania", "Luxembourg", "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall Islands", "Mauritania",
            "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco", "Mozambique", "Myanmar",
            "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand", "Nicaragua", "Niger", "Nigeria", "North Korea", "North Macedonia",
            "Norway", "Oman", "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines",
            "Poland", "Portugal", "Qatar", "Romania", "Russia", "Rwanda", "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent and the Grenadines", "Samoa",
            "San Marino", "Sao Tome and Principe", "Saudi Arabia", "Senegal", "Serbia", "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia",
            "Solomon Islands", "Somalia", "South Africa", "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname", "Sweden",
            "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo", "Tonga", "Trinidad and Tobago",
            "Tunisia", "Turkey", "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "Uruguay",
            "Uzbekistan", "Vanuatu", "Vatican City", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
    );

    public static boolean isValidCountry(String country) {
        return COUNTRIES.contains(country);
    }

    public static boolean doesStringHaveDigit(String input) {

        for (char c : input.toCharArray()) {

            if (Character.isDigit(c)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAtLeast16YearsOld(LocalDate dateOfBirth) {

        if (dateOfBirth == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        Period age = Period.between(dateOfBirth, today);

        return age.getYears() >= 16;
    }

    public static boolean isValidTimeZone(String timeZone) {
        try {
            ZoneId.of(timeZone);
            return true;  // Valid time zone
        } catch (Exception e) {
            return false; // Invalid time zone
        }
    }

    public static boolean isValidEmail(String name) {
        if (name == null) {
            return false;
        }

        String regex = Constants.EMAIL_REGEX;

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(name);

        return m.matches();
    }

    public static boolean isValidPhoneLocalPhoneNumber(String name) {

        String regex = Constants.ZIM_NUMBER_REGEX;

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(name);

        return m.matches();
    }

    public static boolean isValidInternationalPhoneNumber(String phoneNumber)
    {
        if (phoneNumber == null) {
            return false;
        }

        String regex = Constants.INTERNATIONAL_PHONE_REGEX;

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(phoneNumber);

        return m.matches();
    }

    public static boolean isValidNationalIdNumber(String nationalIdNumber)
    {
        String regex = Constants.NATIONAL_ID_REGEX;

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(nationalIdNumber);

        return m.matches();
    }

    public static boolean isValidUserName(String name) {

        String regex = Constants.USERNAME_REGEX;

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(name);

        return m.matches();
    }

    public static boolean isPasswordValid(String password)
    {
        if (password == null) {
            return false;
        }

        String regex = Constants.PASSWORD_REGEX;

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(password);

        return m.matches();
    }

    public static boolean isValidGender(String value) {

        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {

            Gender.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {

            return false;
        }
    }

    public static boolean isValidFileType(String value) {

        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {

            FileType.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {

            return false;
        }
    }

    public static boolean isValidOwnerType(String value) {

        if (value == null || value.trim().isEmpty())
            return false;

        try {

            OwnerType.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {

            return false;
        }
    }

    public static boolean isValidStorageProvider(String value) {

        if (value == null || value.trim().isEmpty())
            return false;

        try {

            StorageProvider.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {

            return false;
        }
    }

    public static boolean isValidVerificationMethod(String value) {

        if (value == null || value.trim().isEmpty())
            return false;

        try {

            VerificationMethod.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {

            return false;
        }
    }

    public static boolean isValidVerificationSource(String value) {

        if (value == null || value.trim().isEmpty())
            return false;

        try {

            VerificationSource.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {

            return false;
        }
    }

    public static boolean isValidVerificationTargetType(String value) {

        if (value == null || value.trim().isEmpty())
            return false;

        try {

            VerificationTargetType.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {

            return false;
        }
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNullOrLessThanOne(Long value) {
        return value == null || value < 1L;
    }

    public static boolean isNullOrEmpty(List<String> list) {
        return list == null || list.isEmpty() || list.stream().allMatch(s -> s == null || s.trim().isEmpty());
    }

    public static boolean isNull(Boolean value) {
        return value == null;
    }

    public static boolean isNullOrEmpty(LocalDateTime dateTime) {
        return dateTime == null;
    }

    public static String capitalizeWords(String input) {

        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder capitalized = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return capitalized.toString().trim();
    }
}
