import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestCaseManager {


    private final List<TestCase> testCases;
    private final boolean prints;


    public TestCaseManager() {
        this.testCases = new ArrayList<>();
        this.prints = true;
    }

    public TestCaseManager(boolean willPrint) {
        this.testCases = new ArrayList<>();
        this.prints = willPrint;
    }


    public int score() {
        int score = 0;
        for (TestCase testCase : this.testCases) {
            if (testCase.getScore() == testCase.getOrders().size())
                score++;
        }
        return score;
    }

    public int size() {
        return this.testCases.size();
    }

    public int ordersScore() {
        int score = 0;
        for (TestCase testCase : this.testCases)
            score += testCase.getScore();
        return score;
    }

    public int ordersSize() {
        int size = 0;
        for (TestCase testCase : this.testCases)
            size += testCase.getOrders().size();
        return size;
    }

    public List<TestCase> getTestCases() {
        return this.testCases;
    }

    public boolean willPrint() {
        return this.prints;
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


    public static void main(String[] args) {

        //example();
        // See `example_main.txt` for contents of `example()`

        System.out.println("\n----------------------------------------\n");

        TestCaseManager manager = new TestCaseManager(true);
        FileTestCaseParser fileParser = new DATCFileParser();  // Will grab from "src/testgames/" directory by default

        Collection<TestCase> testCases = fileParser.parseMany();
        manager.testCases.addAll(testCases);
        System.out.println("\n----------------------------------------\n");
        for (TestCase testCase : manager.testCases)
            testCase.eval(manager.willPrint());

        System.out.println("----------------------------------------\n");
        for (TestCase testCase : manager.testCases) {
            testCase.printNameAndScore();
            if (testCase.getScore() != testCase.getSize())
                System.out.println("\t\u001B[31mFAILED!!\u001B[0m");  // red color ANSI code (then black)
        }

        System.out.println("\n----------------------------------------");
        System.out.printf("TOTAL SCORE (by Test Cases):\t[%d/%d]\n", manager.score(), manager.size());
        System.out.printf("TOTAL SCORE (by Orders):\t\t[%d/%d]\n", manager.ordersScore(), manager.ordersSize());
        System.out.println("----------------------------------------\n");

    }


}