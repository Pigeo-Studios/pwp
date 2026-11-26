package com.pwp.api.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Фильтр мата и оскорблений чата (этап 10 ROADMAP). Слово сообщения нормализуется (регистр, ё→е, латинские и
 * цифровые «двойники» букв → кириллица, разделители внутри слова убираются, повторы букв схлопываются) и
 * сверяется с правилами; совпавшее слово заменяется звёздочками той же длины, остальное сообщение не трогается.
 *
 * <p>Правило — строка файла {@code config/pwp_chat_filter.txt}: {@code корень} — где угодно в слове,
 * {@code ^корень} — начало слова, {@code =слово} — слово целиком; {@code #} — комментарий. Корни нарочно
 * короткие и с якорями: «оскорблять», «застрахуем», «хлебала» и «жидкость» не должны краснеть.
 */
public final class ChatFilter {

    /** Список по умолчанию (файл создаётся с ним; админ дописывает своё). */
    public static final List<String> DEFAULT_RULES = List.of(
            "# мат",
            "^хуй",
            "^хуе",
            "^хуя",
            "^хую",
            "^хуи",
            "^нахуй",
            "^нахуя",
            "^похуй",
            "^охуе",
            "^охуи",
            "пизд",
            "^еба",
            "^ебл",
            "^ебу",
            "^ебн",
            "^уеб",
            "^заеб",
            "^наеб",
            "^отъеб",
            "^съеб",
            "^выеб",
            "^доеб",
            "^поеб",
            "^бляд",
            "=бля",
            "=блять",
            "=сука",
            "=суки",
            "^сучар",
            "^сучк",
            "^мудак",
            "^мудил",
            "^пидор",
            "^пидар",
            "^педик",
            "гандон",
            "^шлюх",
            "^fuck",
            "^shit",
            "^bitch",
            "^cunt",
            "^fag",
            "# оскорбления по национальности",
            "^хохол",
            "^хохл",
            "^кацап",
            "^москал",
            "^чурк",
            "^нигер",
            "^нигг",
            "^nigg");

    private enum Kind {
        ANY,
        START,
        EXACT
    }

    private record Rule(Kind kind, String text) {
        boolean matches(String word) {
            return switch (kind) {
                case ANY -> word.contains(text);
                case START -> word.startsWith(text);
                case EXACT -> word.equals(text);
            };
        }
    }

    /** Итог: текст со звёздочками и было ли что-то скрыто. */
    public record Result(String text, boolean masked) {}

    private final List<Rule> rules;

    public ChatFilter(List<String> lines) {
        List<Rule> out = new ArrayList<>();
        for (String raw : lines) {
            String l = raw.strip();
            if (l.isEmpty() || l.startsWith("#")) continue;
            Kind k = Kind.ANY;
            if (l.startsWith("^")) {
                k = Kind.START;
                l = l.substring(1);
            } else if (l.startsWith("=")) {
                k = Kind.EXACT;
                l = l.substring(1);
            }
            String n = normalize(l);
            if (!n.isEmpty()) out.add(new Rule(k, n));
        }
        this.rules = List.copyOf(out);
    }

    public static ChatFilter defaults() {
        return new ChatFilter(DEFAULT_RULES);
    }

    public int size() {
        return rules.size();
    }

    public Result apply(String message) {
        if (message == null || message.isEmpty() || rules.isEmpty()) return new Result(message, false);
        StringBuilder out = new StringBuilder(message.length());
        boolean masked = false;
        int i = 0;
        int n = message.length();
        while (i < n) {
            if (Character.isWhitespace(message.charAt(i))) {
                out.append(message.charAt(i++));
                continue;
            }
            int j = i;
            while (j < n && !Character.isWhitespace(message.charAt(j))) j++;
            String token = message.substring(i, j);
            if (bad(normalize(token))) {
                // знаки по краям слова («сука,» «!блин») остаются, звёздочки — только на само слово
                int a = 0;
                int z = token.length();
                while (a < z && !Character.isLetterOrDigit(token.charAt(a))) a++;
                while (z > a && !Character.isLetterOrDigit(token.charAt(z - 1))) z--;
                out.append(token, 0, a).append("*".repeat(z - a)).append(token, z, token.length());
                masked = true;
            } else {
                out.append(token);
            }
            i = j;
        }
        return new Result(out.toString(), masked);
    }

    private boolean bad(String word) {
        if (word.isEmpty()) return false;
        for (Rule r : rules) if (r.matches(word)) return true;
        return false;
    }

    /** Нижний регистр, двойники → кириллица, только буквы, повторы схлопнуты («Х.У.Й», «xyйййй» → «хуй»). */
    public static String normalize(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder(lower.length());
        char prev = 0;
        for (int i = 0; i < lower.length(); i++) {
            char c = twin(lower.charAt(i));
            if (!Character.isLetter(c)) continue;
            if (c == prev) continue;
            b.append(c);
            prev = c;
        }
        return b.toString();
    }

    /**
     * Латиница и цифры, которыми пишут кириллицу («xyй», «cyka», «3» = з). Правила нормализуются той же
     * функцией, поэтому английские корни («fuck» → «fuск») совпадают с сообщениями так же надёжно.
     */
    private static char twin(char c) {
        return switch (c) {
            case 'ё' -> 'е';
            case 'a', '@' -> 'а';
            case 'e', '3' -> c == '3' ? 'з' : 'е';
            case 'o', '0' -> 'о';
            case 'p' -> 'р';
            case 'c' -> 'с';
            case 'x' -> 'х';
            case 'y' -> 'у';
            case 'k' -> 'к';
            case 'm' -> 'м';
            case 'h' -> 'н';
            case 'b', '6' -> 'б';
            case 't' -> 'т';
            case '4' -> 'ч';
            default -> c;
        };
    }
}
