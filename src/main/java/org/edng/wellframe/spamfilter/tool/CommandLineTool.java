package org.edng.wellframe.spamfilter.tool;

import org.edng.wellframe.spamfilter.configuration.ApplicationConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Command line tool to manually feed email messages into the system for training.
 *
 * Created by ed on 2/8/15.
 */
public class CommandLineTool {

    public static void main(String[] args) throws Exception {
        // load Spring context
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(ApplicationConfiguration.class);
        ctx.refresh();

        // load beans
        NaiveBayesSpamFilter naiveBayesSpamFilter = ctx.getBean(NaiveBayesSpamFilter.class);
        EmailParser emailParser = ctx.getBean(EmailParser.class);
        HtmlTextExtractor htmlTextExtractor = ctx.getBean(HtmlTextExtractor.class);

        if (args == null || args.length < 2) {
            printHelp();
            return;
        }

        File dir = new File(args[0]);
        if (!dir.exists()) {
            System.out.println("Directory " + dir + " is not accessible");
        }

        boolean isSpam = args[1].toLowerCase().startsWith("y") || args[1].equals("1");

        String lineSeparator = System.getProperty("line.separator");

        File[] files = dir.listFiles();

        for (File file : files) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ( (line = reader.readLine()) != null ) {
                sb.append(line);
                sb.append(lineSeparator);
            }
            // extract email body and text from html
            String content = htmlTextExtractor.getTextByHtml(emailParser.getBody(sb.toString()));
            // feed content to naiveBayesSpamFilter for training,
            // stats will be merged at the end
            naiveBayesSpamFilter.train(content, isSpam, false);
        }

        naiveBayesSpamFilter.mergeStats();
    }

    public static void printHelp() {
        System.out.println("CommadLineTool <directory> <isSpam>");
    }

}
