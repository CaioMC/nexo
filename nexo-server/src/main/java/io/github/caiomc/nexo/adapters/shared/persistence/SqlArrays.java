package io.github.caiomc.nexo.adapters.shared.persistence;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;

/**
 * Conversão de {@code text[]} do Postgres.
 */
public final class SqlArrays {

    private SqlArrays() {
    }

    public static List<String> strings(Array array) throws SQLException {
        if (isNull(array)) {
            return null;
        }

        return Arrays.asList((String[]) array.getArray());
    }

    public static String[] array(List<String> values) {
        return values.toArray(String[]::new);
    }
}
