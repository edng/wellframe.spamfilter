package org.edng.wellframe.spamfilter.tool;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Using Naive Bayes technique to detect spam messages
 *
 * Created by ed on 2/7/15.
 */
@Component
public class NaiveBayesSpamFilter {
    protected Logger log = Logger.getLogger(FileHandler.class);

    @Resource
    private FileHandler fileHandler;

    public static final String[] STOP_WORDS = {"but", "be", "with", "such", "then", "for", "no", "will", "not", "are", "and", "their", "if", "this", "on", "into", "a", "or", "there", "in", "that", "they", "was", "is", "it", "an", "the", "as", "at", "these", "by", "to", "of"};

    private Set<String> stopWordSet;
    private long totalSpam;
    private long totalHam;
    private long totalSpamTokens;
    private long totalHamTokens;
    private Map<String, Long> spamTokenCount;
    private Map<String, Long> hamTokenCount;

    public NaiveBayesSpamFilter() {
    }

    @PostConstruct
    public void init() {
        loadStats();
    }

    public void clearStats() {
        log.info("Clearing stats");
        stopWordSet = new HashSet();
        for (String word : STOP_WORDS) {
            stopWordSet.add(word);
        }
        totalSpam = 0;
        totalHam = 0;
        totalSpamTokens = 0;
        totalHamTokens = 0;
        spamTokenCount = new HashMap();
        hamTokenCount = new HashMap();
    }

    /**
     * Persist stats on disk
     *
     * @throws IOException
     */
    protected void saveStats() throws IOException {
        log.info("Saving stats");
        // save stop words
        StringBuilder stopWordsString = new StringBuilder();
        for (String stopWord : stopWordSet) {
            stopWordsString.append(stopWord);
            stopWordsString.append("\n");
        }
        fileHandler.saveStatsContent(stopWordsString.toString(), "stopwords.txt");

        Properties properties = new Properties();

        // save totals
        properties.setProperty("totalSpam", totalSpam+"");
        properties.setProperty("totalHam", totalHam+"");
        properties.setProperty("totalSpamTokens", totalSpamTokens+"");
        properties.setProperty("totalHamTokens", totalHamTokens+"");
        fileHandler.saveStatsProperties(properties, "stats.properties");

        // save spam tokens
        properties.clear();
        for (Map.Entry<String, Long> entry : spamTokenCount.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue().toString());
        }
        fileHandler.saveStatsProperties(properties, "spamtokens.properties");

        // save ham tokens
        properties.clear();
        for (Map.Entry<String, Long> entry : hamTokenCount.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue().toString());
        }
        fileHandler.saveStatsProperties(properties, "hamtokens.properties");
        log.info("Saving stats completed");
    }

    /**
     * Load stats from disk to memory
     */
    public void loadStats() {
        log.info("Loading stats");
        clearStats();
        // load stop words
        BufferedReader reader;
        try {
            reader = fileHandler.loadStatsContent("stopwords.txt");
            String line;
            while ((line = reader.readLine()) != null) {
                stopWordSet.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            // leave stop word set as default
        }
        if (stopWordSet.isEmpty()) {
            for (String word : STOP_WORDS) {
                stopWordSet.add(word);
            }
        }
        log.info("Loaded stop words");

        Properties properties = new Properties();

        // load totals
        try {
            fileHandler.loadStatsProperties("stats.properties", properties);
        } catch (IOException e) {
            // leave values as default
        }
        try {
            totalSpam = new Long(properties.getProperty("totalSpam"));
        } catch (NumberFormatException e) {
        }
        try {
            totalHam = new Long(properties.getProperty("totalHam"));
        } catch (NumberFormatException e) {
        }
        try {
            totalSpamTokens = new Long(properties.getProperty("totalSpamTokens"));
        } catch (NumberFormatException e) {
        }
        try {
            totalHamTokens = new Long(properties.getProperty("totalHamTokens"));
        } catch (NumberFormatException e) {
        }
        log.info("Loaded totals: totalSpam="+totalSpam+",totalHam="+totalHam+",totalSpamTokens="+totalSpamTokens+",totalHamTokens="+totalHamTokens);

        // load spam tokens
        try {
            fileHandler.loadStatsProperties("spamtokens.properties", properties);
            for (String token : properties.stringPropertyNames()) {
                spamTokenCount.put(token, new Long(properties.getProperty(token)));
            }
        } catch (IOException e) {
            // leave empty
        }
        log.info("Loaded spam tokens");

        // load ham tokens
        try {
            fileHandler.loadStatsProperties("hamtokens.properties", properties);
            for (String token : properties.stringPropertyNames()) {
                hamTokenCount.put(token, new Long(properties.getProperty(token)));
            }
        } catch (IOException e) {
            // leave empty
        }
        log.info("Loaded ham tokens");
        log.info("Loading stats completed");
    }

    /**
     * Merge processed messages stats
     *
     * @throws IOException
     */
    public void mergeStats() throws IOException {
        log.info("Merging stats");
        mergeStatsSpam();
        mergeStatsHam();
        log.info("Merging stats completed");
    }

    /**
     * Merge spam messages stats
     *
     * @throws IOException
     */
    protected synchronized void mergeStatsSpam() throws IOException {
        log.info("Merging stats spam");
        // load outstanding spam stats
        String[] messages = fileHandler.listMessages(true);
        Properties properties = new Properties();

        for (String message : messages) {
            log.debug("message="+message);
            totalSpam++;
            // load stats
            properties.clear();
            fileHandler.loadMessageProperties(properties, message + "_stats.properties", true);
            totalSpamTokens += new Long(properties.getProperty("totalTokens"));
            log.debug("message="+message+": totalSpamTokens="+totalSpamTokens);
            properties.clear();
            fileHandler.loadMessageProperties(properties, message + "_tokens.properties", true);
            for (String token : properties.stringPropertyNames()) {
                if (spamTokenCount.containsKey(token)) {
                    long count = spamTokenCount.get(token);
                    count += new Long(properties.getProperty(token));
                    spamTokenCount.put(token, count);
                } else {
                    spamTokenCount.put(token,
                            new Long(properties.getProperty(token)));
                }
            }
            fileHandler.moveFilesToArchive(message, true);
            log.debug("message="+message+": Moved to archive");
            fileHandler.markMessageMerged(message);
            log.debug("message="+message+": Marked merged");
            saveStats();
            log.debug("message="+message+": Stats saved");
        }
        log.info("Merging stats spam completed");
    }

    /**
     * Merge ham message stats
     *
     * @throws IOException
     */
    protected synchronized void mergeStatsHam() throws IOException {
        log.info("Merging stats ham");
        // load outstanding spam stats
        String[] messages = fileHandler.listMessages(false);
        Properties properties = new Properties();

        for (String message : messages) {
            log.debug("message="+message);
            totalHam++;
            // load stats
            properties.clear();
            fileHandler.loadMessageProperties(properties, message + "_stats.properties", false);
            totalHamTokens += new Long(properties.getProperty("totalTokens"));
            log.debug("message="+message+": totalHamTokens="+totalHamTokens);
            properties.clear();
            fileHandler.loadMessageProperties(properties, message + "_tokens.properties", false);
            for (String token : properties.stringPropertyNames()) {
                if (hamTokenCount.containsKey(token)) {
                    long count = hamTokenCount.get(token);
                    count += new Long(properties.getProperty(token));
                    hamTokenCount.put(token, count);
                } else {
                    hamTokenCount.put(token,
                            new Long(properties.getProperty(token)));
                }
            }
            fileHandler.moveFilesToArchive(message, false);
            log.debug("message="+message+": Moved to archive");
            fileHandler.markMessageMerged(message);
            log.debug("message=" + message + ": Marked merged");
            saveStats();
            log.debug("message=" + message + ": Stats saved");
        }
        log.info("Merging stats ham completed");
    }

    /**
     * Accept a message to train the system.  Message needs to be marked as either spam
     * or not and the stats will go to appropriate buckets: total tokens, per token count.
     *
     * @param message
     * @param isSpam
     * @param commit if set to true, it will call saveStats after message is processed
     * @throws IOException
     */
    public void train(String message, boolean isSpam, boolean commit) throws IOException {
        String md5Filename = fileHandler.generateMd5Filename(message);
        log.debug("Training start: " + md5Filename);

        if (fileHandler.isMessageMerged(md5Filename)) {
            log.debug("Already processed " + md5Filename + ", skipping");
            return;
        }

        long totalTokens = 0;
        Map<String, Long> tokenCount = new HashMap();
        TermFilter termFilter = new TermFilter(new StringReader(message), stopWordSet);
        String token;
        while ( (token = termFilter.nextToken()) != null ) {
            if (tokenCount.containsKey(token)) {
                tokenCount.put(token, new Long(tokenCount.get(token) + 1));
            } else {
                tokenCount.put(token, new Long(1));
            }
            totalTokens++;
        }

        // save stats to file
        Properties properties = new Properties();

        // save totals
        properties.setProperty("totalTokens", totalTokens+"");

        fileHandler.saveMessageProperties(properties, md5Filename + "_stats.properties", isSpam);

        properties.clear();

        // save spam tokens
        for (Map.Entry<String, Long> entry : tokenCount.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue().toString());
        }
        fileHandler.saveMessageProperties(properties, md5Filename + "_tokens.properties", isSpam);

        // save file indicator for completeness
        fileHandler.saveMessageContent("", md5Filename + ".complete", isSpam);

        if (commit) {
            mergeStats();
        }
        log.debug("Training ends");
    }

    /**
     * Calculate message's probability of being spam using this formula
     *
     * Overall spam messages probability - sp
     * Overall ham messages probability - hp
     * Message spam probability by tokens - msp
     * Message ham probability by tokens - mhp
     *
     * msp is calculated by tokens then multiplies together as followed
     *   msp *= (token_count + 1) / (total_spam_token_count + 1)
     * mhp is calculated by tokens then multiplies together as followed
     *   mhp *= (token_count + 1) / (total_ham_token_count + 1)
     *
     * Note in both msp and mhp calculation, it adds 1 to both numerator
     * and denominator for smoothing purpose.
     *
     * Spam probability = (sp * msp) / ( (sp * msp) + (hp * mhp) )
     *
     * @param message
     * @return
     */
    public float spamProbability(String message) {
        BigDecimal spamMessagesProbability = new BigDecimal(totalSpam).divide(
                new BigDecimal(totalSpam + totalHam), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal hamMessagesProbability = new BigDecimal(totalHam).divide(
                new BigDecimal(totalSpam + totalHam), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalSpamTokens = new BigDecimal(this.totalSpamTokens);
        BigDecimal totalHamTokens = new BigDecimal(this.totalHamTokens);
        BigDecimal messageIsSpamProbability = BigDecimal.ONE;
        BigDecimal messageIsHamProbability = BigDecimal.ONE;

        TermFilter termFilter = new TermFilter(new StringReader(message), stopWordSet);
        String token;
        while ( (token = termFilter.nextToken()) != null ) {
            if (spamTokenCount.containsKey(token)) {
                messageIsSpamProbability = messageIsSpamProbability.multiply(
                        new BigDecimal(spamTokenCount.get(token)).add(BigDecimal.ONE)
                                .divide(
                                        totalSpamTokens.add(BigDecimal.ONE), 6, BigDecimal.ROUND_HALF_UP
                                )
                        );
            } else {
                messageIsSpamProbability = messageIsSpamProbability.multiply(
                        BigDecimal.ONE
                                .divide(
                                        totalSpamTokens.add(BigDecimal.ONE), 6, BigDecimal.ROUND_HALF_UP
                                )
                );
            }
            if (hamTokenCount.containsKey(token)) {
                messageIsHamProbability = messageIsHamProbability.multiply(
                        new BigDecimal(hamTokenCount.get(token)).add(BigDecimal.ONE)
                                .divide(
                                        totalHamTokens.add(BigDecimal.ONE), 6, BigDecimal.ROUND_HALF_UP
                                )
                );
            } else {
                messageIsHamProbability = messageIsHamProbability.multiply(
                        BigDecimal.ONE
                                .divide(
                                        totalHamTokens.add(BigDecimal.ONE), 6, BigDecimal.ROUND_HALF_UP
                                )
                );
            }
        }

        BigDecimal overallSpamProbability = spamMessagesProbability.multiply(messageIsSpamProbability).divide(
                spamMessagesProbability.multiply(messageIsSpamProbability).add(
                        hamMessagesProbability.multiply(messageIsHamProbability)
                ), 6, BigDecimal.ROUND_HALF_UP
        );

        return overallSpamProbability.floatValue();
    }
}
