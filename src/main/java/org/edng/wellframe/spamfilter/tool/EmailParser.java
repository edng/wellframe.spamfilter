package org.edng.wellframe.spamfilter.tool;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Created by ed on 2/9/15.
 */
@Component
public class EmailParser {
    protected Logger log = Logger.getLogger(FileHandler.class);

    public EmailParser() {

    }

    /**
     * Poor man's approach in extract email body by delimiting
     * by \n\n
     * @param message Email message in RFC822 format
     * @return Email body
     */
    public String getBody(String message) {
        String lineSeparator = System.getProperty("line.separator");
        int offset = message.indexOf(lineSeparator+lineSeparator);
        return offset > 0 ? message.substring(offset) : message;
    }
}
