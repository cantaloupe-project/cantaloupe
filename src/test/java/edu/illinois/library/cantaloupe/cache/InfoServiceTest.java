package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.MockFileProcessor;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

public class InfoServiceTest extends BaseTest {

    private InfoService instance;

    @Before
    public void setUp() {
        InfoService.clearInstance();
        instance = InfoService.getInstance();
    }

    private FileProcessor newProcessor() throws Exception {
        FileProcessor proc = new MockFileProcessor();
        proc.setSourceFormat(Format.JPG);
        proc.setSourceFile(TestUtil.getImage("jpg"));
        return proc;
    }

    @Test
    public void testGetOrReadInfoWithHitInMemoryCache() throws Exception {
        instance.getOrReadInfo(new Identifier("bogus"), newProcessor());
    }

    @Test
    public void testGetOrReadInfoWithHitInDerivativeCache() {
        // TODO: write this
    }

    @Test
    public void testGetOrReadInfoWithEmptyCaches() {
        // TODO: write this
    }

}
