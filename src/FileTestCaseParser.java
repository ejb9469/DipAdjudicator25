import java.util.Collection;

/**
 * Interface describing the functionality of a test case parser using file(s) as input
 */
public interface FileTestCaseParser extends TestCaseParser {

    @Override
    TestCase                parse(String source);

    Collection<TestCase>    parseMany();

}
