import java.util.Collection;

/**
 * Interface describing the functionality of a test case parser (from String -> `TestCase`)
 */
public interface TestCaseParser {

    TestCase parse(String source);

}