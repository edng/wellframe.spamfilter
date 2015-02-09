package org.edng.wellframe.spamfilter.tool;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * This class uses Boilerpipe libray (https://code.google.com/p/boilerpipe/)
 * and Jsoup to extract text from HTML.
 *
 * Created by ed on 2/7/15.
 */
@Component
public class HtmlTextExtractor {
    protected Logger log = Logger.getLogger(FileHandler.class);

    private ArticleExtractor articleExtractor;

    public HtmlTextExtractor() {
        articleExtractor = ArticleExtractor.INSTANCE;
    }

    /**
     * Extract text from HTML first with BoilerPipe, then with
     * Jsoup.
     * @param html HTML string to be analyzed for text extraction
     * @return Extracted text from HTML string
     */
    public String getTextByHtml(String html) {
        String text = null;
        try {
            text = articleExtractor.getText(html);
        } catch (BoilerpipeProcessingException e) {
            text = null;
        }
        if (text == null || text.isEmpty()) {
            text = Jsoup.parse(html).text();
        }
        if (text == null || text.isEmpty()) {
            return html;
        }
        return text;
    }
}
