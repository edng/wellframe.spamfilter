package org.edng.wellframe.spamfilter.web.controller;

import org.edng.wellframe.spamfilter.tool.EmailParser;
import org.edng.wellframe.spamfilter.tool.HtmlTextExtractor;
import org.edng.wellframe.spamfilter.tool.NaiveBayesSpamFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.NumberFormat;

/**
 * Created by ed on 2/7/15.
 */
@Controller
public class ApiController {
    @Resource
    private HtmlTextExtractor htmlTextExtractor;
    @Resource
    private EmailParser emailParser;
    @Resource
    private NaiveBayesSpamFilter naiveBayesSpamFilter;

    @RequestMapping(value = "/check-message", method = { RequestMethod.GET, RequestMethod.POST } )
    public @ResponseBody String checkMessage(@RequestParam("message") String message) {
        // extract text from email body and html
        String content = htmlTextExtractor.getTextByHtml(emailParser.getBody(message));
        float spamProbability = naiveBayesSpamFilter.spamProbability(content);
        if (spamProbability >= 0.5f) {
            return "SPAM ("+ NumberFormat.getPercentInstance().format(spamProbability) + ")";
        }
        return "NOT SPAM ("+ NumberFormat.getPercentInstance().format(1f - spamProbability) + ")";
    }

    @RequestMapping(value = "/mark-message", method = { RequestMethod.GET, RequestMethod.POST } )
    public @ResponseBody String markMessage(@RequestParam("message") String message,
                                 @RequestParam("isSpam") String isSpam) {
        String content = htmlTextExtractor.getTextByHtml(emailParser.getBody(message));
        boolean spam = isSpam.toLowerCase().startsWith("y") || isSpam.equals("1");
        try {
            naiveBayesSpamFilter.train(content, spam, true);
        } catch (IOException e) {
            return "Failed to process message";
        }
        return "Success";
    }

}
