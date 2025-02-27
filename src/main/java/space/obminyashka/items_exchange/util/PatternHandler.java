package space.obminyashka.items_exchange.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatternHandler {

    public static final String PASSWORD = "(?=.*?[0-9])(?=.*?[a-z])(?=.*?[A-Z])(?=\\S+$)[\\w\\p{Punct}]+";
    public static final String PHONE_NUMBER =
            "^\\s*(?<country>\\+?\\d{2})[-. (]*(?<area>\\d{3})[-. )]*(?<number>\\d{3}[-. ]*\\d{2}[-. ]*\\d{2})\\s*$";
    public static final String USERNAME = "(?=\\S+$)[\\wА-Яа-яЁёҐЄІЇієїґ\\p{Punct}&&[^@]]+";
    public static final String WORD_EMPTY_OR_MIN_2_MAX_50 = "^$|^[\\wА-Яа-я-'`ЁёҐЄІЇієїґ]{2,50}$";
}
