package org.edng.wellframe.spamfilter.tool;

import org.apache.log4j.Logger;

import java.io.Reader;
import java.util.Scanner;
import java.util.Set;

/**
 * This provides a consistent filtering layer to tokenize and massage
 * term before it's fed to the system.
 *
 * Created by ed on 2/8/15.
 */
public class TermFilter {
    protected Logger log = Logger.getLogger(FileHandler.class);

    public static final String SPACE_DELIMITER = " ";
    private Scanner scanner;
    private Set<String> stopWordsSet;

    /**
     * Tokenize by default on space character and default stop words
     * @param reader
     * @param stopWordsSet
     */
    public TermFilter(Reader reader, Set<String> stopWordsSet) {
        this(reader, SPACE_DELIMITER, stopWordsSet);
    }

    public TermFilter(Reader reader, String delimiter, Set < String > stopWordsSet) {
        scanner = new Scanner(reader);
        scanner.useDelimiter(delimiter);
        this.stopWordsSet = stopWordsSet;
    }

    /**
     * Return next token after cleaning up term and filtered by stop words
     * @return
     */
    public String nextToken() {
        if (!scanner.hasNext()) {
            return null;
        }
        String token = scanner.next().trim().toLowerCase();
        if (token.isEmpty()) {
            return null;
        }
        if (stopWordsSet.contains(token)) {
            return this.nextToken();
        }
        return token;
    }
}
