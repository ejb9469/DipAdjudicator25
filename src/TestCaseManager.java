import java.util.ArrayList;
import java.util.List;

public class TestCaseManager {


    private final List<TestCase> testCases;
    private boolean prints;


    public TestCaseManager() {
        this.testCases = new ArrayList<>();
        this.prints = true;
    }

    public TestCaseManager(boolean isPrinting) {
        this.testCases = new ArrayList<>();
        this.prints = isPrinting;
    }


    public List<TestCase> getTestCases() {
        return this.testCases;
    }

    public boolean prints() {
        return this.prints;
    }

    public void togglePrint() {
        this.prints = !prints;
    }


    public void addTestCaseWithFields(TestCase testCase, boolean evalNow, boolean... expectedFields) {
        testCase.setExpectedFields(expectedFields);
        this.testCases.add(testCase);
        if (evalNow)
            testCase.eval(this.prints);
    }

    public void addTestCaseWithFields(TestCase testCase, boolean evalNow, boolean[]... expectedFields) {
        testCase.setExpectedFields(expectedFields);
        this.testCases.add(testCase);
        if (evalNow)
            testCase.eval(this.prints);
    }


    public int score() {
        int score = 0;
        for (TestCase testCase : this.testCases)
            score += testCase.getScore();
        return score;
    }

    public int size() {
        int size = 0;
        for (TestCase testCase : this.testCases)
            size += testCase.getOrders().size();
        return size;
    }


    public static void main(String[] args) {

        //example();
        // See `example_main.txt` for contents of `example()`

        System.out.println("\n----------------------------------------");

        TestCaseManager manager = new TestCaseManager(true);
        DATCFileParser fileParser = new DATCFileParser();  // Will grab from "src/testgames/" directory by default

        List<TestCase> testCases = fileParser.parseMany();
        manager.testCases.addAll(testCases);
        System.out.println("----------------------------------------\n\n");
        for (TestCase testCase : manager.testCases)
            testCase.eval(manager.prints());

        System.out.println("----------------------------------------\n");
        System.out.printf("TOTAL SCORE: [%d/%d]\n", manager.score(), manager.size());

    }


}