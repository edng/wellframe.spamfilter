package org.edng.wellframe.spamfilter;

import org.apache.commons.io.FileUtils;
import org.edng.wellframe.spamfilter.configuration.ApplicationConfiguration;
import org.edng.wellframe.spamfilter.tool.FileHandler;
import org.edng.wellframe.spamfilter.tool.NaiveBayesSpamFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;


/**
 * Created by ed on 2/7/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class})
public class NaiveBayesSpamFilterTest {
    @Resource
    private NaiveBayesSpamFilter naiveBayesSpamFilter;
    @Resource
    private FileHandler fileHandler;

    @Test
    public void testSpamProbability() throws IOException {
        String dir = "testdata";
        FileUtils.deleteDirectory(new File(dir));
        fileHandler.setMessagesDirectory(dir + "/messages");
        fileHandler.setStatsDirectory(dir + "/stats");

        naiveBayesSpamFilter.clearStats();

        naiveBayesSpamFilter.train("Humpty Dumpty sat on a wall", true, false);
        naiveBayesSpamFilter.train("Humpty Dumpty had a great fall", true, false);
        naiveBayesSpamFilter.train("All the king's horses and all the king's men", false, false);
        naiveBayesSpamFilter.train("Couldn't put Humpty together again", false, false);

        naiveBayesSpamFilter.mergeStats();

        float f = naiveBayesSpamFilter.spamProbability("humpty dumpty");

        assertTrue("\"humpty dumpty\" should be greater than or equals 0.5: " + f, f >= 0.5f);

        f = naiveBayesSpamFilter.spamProbability("all the king");

        assertTrue("\"horse king\" should be less than 0.5: " + f, f < 0.5f);

        FileUtils.deleteDirectory(new File(dir));
    }
}
