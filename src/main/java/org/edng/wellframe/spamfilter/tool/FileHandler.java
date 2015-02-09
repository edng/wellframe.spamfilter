package org.edng.wellframe.spamfilter.tool;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * This class provides the utilities to handle all file operations
 * including saving and loading of Properties files.
 *
 * Message is digested into MD5 so it's saved to file system based on MD5
 * value.  The message file is located in a deep directory structure
 * based on file name where each character in a file represents a
 * sub-directory minus the last character.  For example: file "abcde"
 * will be stored as "a/b/c/d/abcde". We will only go 5 levels deep.
 *
 * Created by ed on 2/7/15.
 */
public class FileHandler {
    protected Logger log = Logger.getLogger(FileHandler.class);

    private MessageDigest md5MessageDigest;
    private File theMessagesSpamDir;
    private File theMessagesHamDir;
    private File theMessagesSpamArchiveDir;
    private File theMessagesHamArchiveDir;
    private File theStatsDir;
    private File theMergedMessagesFile;

    /**
     * Constructor to set up directory structure based on preferred messages
     * and stats directories.
     * @param messagesDir Where we store messages and archive
     * @param statsDir Where we store overall stats
     */
    public FileHandler(String messagesDir, String statsDir) {
        setMessagesDirectory(messagesDir);
        setStatsDirectory(statsDir);
        try {
            md5MessageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 is missing!");
        }
    }

    /**
     * Set up messages directory
     * @param dir
     */
    public void setMessagesDirectory(String dir) {
        theMessagesSpamDir = new File(dir + "/spam");
        theMessagesSpamArchiveDir = new File(dir + "/spam_archive");
        // create directories if they don't exists
        if (!theMessagesSpamDir.exists()) {
            theMessagesSpamDir.mkdirs();
        }
        if (!theMessagesSpamArchiveDir.exists()) {
            theMessagesSpamArchiveDir.mkdirs();
        }
        theMessagesHamDir = new File(dir + "/ham");
        theMessagesHamArchiveDir = new File(dir + "/ham_archive");
        // create directories if they don't exists
        if (!theMessagesHamDir.exists()) {
            theMessagesHamDir.mkdirs();
        }
        if (!theMessagesHamArchiveDir.exists()) {
            theMessagesHamArchiveDir.mkdirs();
        }
    }

    /**
     * Set up the stats directory
     * @param dir
     */
    public void setStatsDirectory(String dir) {
        theStatsDir = new File(dir);
        theMergedMessagesFile = new File(dir + "/messages_completed.txt");
        if (!theStatsDir.exists()) {
            theStatsDir.mkdirs();
        }
        if (!theMergedMessagesFile.exists()) {
            try {
                theMergedMessagesFile.createNewFile();
            } catch (IOException e) {
                throw new AssertionError("Cannot create messages_completed.txt on " + theMergedMessagesFile.getAbsolutePath());
            }
        }
    }

    /**
     * Generate MD5 from content
     * @param content
     * @return
     */
    public String generateMd5Filename(String content) {
        return md5(content);
    }

    /**
     * Check if message is already merged into stats
     * @param messageMd5
     * @return
     * @throws IOException
     */
    public boolean isMessageMerged(String messageMd5) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(theMergedMessagesFile));
        String line;
        while ( (line = reader.readLine()) != null ) {
            if (line.trim().equalsIgnoreCase(messageMd5)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Move files into archive directory under deep directory structure
     * @param messageMd5
     * @param isSpam
     */
    public void moveFilesToArchive(final String messageMd5, boolean isSpam) {
        File dir = isSpam ? theMessagesSpamDir : theMessagesHamDir;
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith(messageMd5);
            }
        });
        File archiveDir = isSpam ? theMessagesSpamArchiveDir : theMessagesHamArchiveDir;
        // pull deep directory structure
        File newDir = setupDirectoryHierarchy(messageMd5, archiveDir);
        for (File file : files) {
            File newFile = new File(newDir.getAbsolutePath() + "/" + file.getName());
            file.renameTo(newFile);
        }
    }

    /**
     * Store processed message MD5 into merged messages file so we can check if message
     * has been processed prior.
     * @param messageMd5
     * @throws IOException
     */
    public void markMessageMerged(String messageMd5) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(theMergedMessagesFile, true));
        printWriter.println(messageMd5);
        printWriter.close();
    }

    /**
     * Save message into file system in either spam or ham folder.
     * @param content
     * @param filename
     * @param isSpam
     */
    public void saveMessageContent(String content, String filename, boolean isSpam) {
        File dir = isSpam ? theMessagesSpamDir : theMessagesHamDir;
        try {
            PrintWriter printWriter = new PrintWriter(dir.getAbsolutePath() + "/" + filename);
            printWriter.print(content);
            printWriter.close();
        } catch (FileNotFoundException e) {
            throw new AssertionError("Directory " + dir.getAbsolutePath() + " does not exist!");
        }
    }

    /*
    public BufferedReader loadStatsContent(String filename, boolean isSpam) {
        File dir = isSpam ? theMessagesSpamDir : theMessagesHamDir;
        File file = new File(dir.getAbsolutePath() + "/" + filename);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            return bufferedReader;
        } catch (IOException e1) {
            throw new AssertionError("Failed to read file " + file.getAbsolutePath() + " from file system! " + e1.getMessage());
        }
    }*/

    /**
     * Save per-message properties into file
     * @param properties
     * @param filename
     * @param isSpam
     */
    public void saveMessageProperties(Properties properties, String filename, boolean isSpam) {
        File dir = isSpam ? theMessagesSpamDir : theMessagesHamDir;
        try {
            properties.store(new FileOutputStream(new File(dir.getAbsolutePath() + "/" + filename)), "");
        } catch (IOException e) {
            throw new AssertionError("Directory " + dir.getAbsolutePath() + " does not exist!");
        }
    }

    /**
     * Loads message properties file
     * @param properties
     * @param filename
     * @param isSpam
     */
    public void loadMessageProperties(Properties properties, String filename, boolean isSpam) {
        File dir = isSpam ? theMessagesSpamDir : theMessagesHamDir;
        try {
            properties.load(new FileInputStream(new File(dir.getAbsolutePath() + "/" + filename)));
        } catch (IOException e) {
            throw new AssertionError("Directory " + dir.getAbsolutePath() + " does not exist!");
        }
    }

    /**
     * List completed messages
     * @param isSpam
     * @return
     */
    public String[] listMessages(boolean isSpam) {
        File dir = isSpam ? theMessagesSpamDir : theMessagesHamDir;
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".complete");
            }
        });
        String[] messages = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            // drop .complete from name
            messages[i] = files[i].getName().substring(0, files[i].getName().length() - ".complete".length());
        }
        return messages;
    }

    /**
     * Save string into a file in stats directory
     * @param content
     * @param filename
     */
    public void saveStatsContent(String content, String filename) {
        try {
            PrintWriter printWriter = new PrintWriter(theStatsDir.getAbsolutePath() + "/" + filename);
            printWriter.print(content);
            printWriter.close();
        } catch (FileNotFoundException e) {
            throw new AssertionError("Directory " + theStatsDir.getAbsolutePath() + " does not exist!");
        }
    }

    /**
     * Save stats properties file into stats directory
     * @param properties
     * @param filename
     */
    public void saveStatsProperties(Properties properties, String filename) {
        try {
            properties.store(new FileOutputStream(new File(theStatsDir.getAbsolutePath() + "/" + filename)), "");
        } catch (IOException e) {
            throw new AssertionError("Directory " + theStatsDir.getAbsolutePath() + " does not exist!");
        }
    }

    /**
     * Load file from stats directory into a BufferedReader
     * @param filename
     * @return
     * @throws IOException
     */
    public BufferedReader loadStatsContent(String filename) throws IOException {
        File file = new File(theStatsDir.getAbsolutePath() + "/" + filename);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        return bufferedReader;
    }

    /**
     * Load properties from stats directory
     * @param filename
     * @param properties
     * @throws IOException
     */
    public void loadStatsProperties(String filename, Properties properties) throws IOException {
        properties.load(new FileInputStream(new File(theStatsDir.getAbsolutePath() + "/" + filename)));
    }

    /**
     * Load a message from file system using the message's MD5 name.
     * @param md5 MD5 representation of the message
     * @return The message
     */
        /*
    public String loadContent(String md5) {
        File messageFile = new File(setupDirectoryHierarchy(md5).getAbsolutePath() + "/" + md5);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile));
            StringBuilder message = new StringBuilder();
            String line;
            while ( (line = bufferedReader.readLine()) != null ) {
                message.append(line);
            }
            return message.toString();
        } catch (IOException e1) {
            throw new AssertionError("Failed to read file " + messageFile.getAbsolutePath() + " from file system! " + e1.getMessage());
        }
    }*/

    /**
     * Using Java's MessageDigest library to generate a MD5 value from a string.
     * MD5 value is return as a hexadecimal string.  This method is synchronized
     * so we can reuse the MessageDigest object.
     * @param content Content to be digested to MD5
     * @return MD5 value as hexadecimal string
     */
    protected synchronized String md5(String content) {
        md5MessageDigest.reset();
        md5MessageDigest.update(content.getBytes(Charset.forName("UTF8")));
        byte[] resultByte = md5MessageDigest.digest();
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : resultByte) {
            stringBuilder.append(String.format("%02X", b));
        }
        return stringBuilder.toString();
    }

    /**
     * This breaks up file name into characters array and create a deep directory
     * structure where each succeeding character represents a sub-directory of
     * previous character.  The purpose of this method is to minimize overloading
     * any single directory with too many files.  Note that the last character
     * is dropped so we may store multiple files in a sub-directory.
     * @param name a file name to be broken up to create a directory structure
     * @return
     */
    protected File setupDirectoryHierarchy(String name, File directory) {
        char[] chars = name.toCharArray();
        StringBuilder deepDir = new StringBuilder();
        deepDir.append(directory.getAbsolutePath());
        deepDir.append("/");
        int length = chars.length > 5 ? 5 : chars.length;
        for (int i = 0; i < length; i++) {
            // only consider the prefix that's assumed to be md5 string
            if (chars[i] == '.' || chars[i] == '_') break;
            deepDir.append(chars[i]);
            deepDir.append("/");
        }
        File theDeepDir = new File(deepDir.toString());
        if (!theDeepDir.exists()) {
            theDeepDir.mkdirs();
        }
        return theDeepDir;
    }
}
