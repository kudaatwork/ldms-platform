package projectlx.user.management.service.utils.generators;

import java.util.Random;

public class AccountNumberAndReferencesGenerator {

    public static String getAccountNumber() {
        Random rand = new Random();
        String card = "";

        for (int i = 0; i < 12; i++)
        {
            int n = rand.nextInt(10) + 0;
            card += Integer.toString(n);
        }

        return card;
    }

    public static Long getTransactionReference() {

        Long l = System.currentTimeMillis();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            builder.append((int) (Math.random() * 10));
        }
        int randomNumber = (int) (Math.random() * 1000);

        String entityId = l + builder.toString() + randomNumber;

        try {
            l = Long.parseLong(entityId);
        } catch (Exception e) {
            return l;
        }
        return l;
    }
}
