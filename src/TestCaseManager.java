import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestCaseManager {


    public static final boolean USE_REFEREE = false;


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

        Constants.printTimestamp();

        TestCaseManager manager = new TestCaseManager(true);
        FileTestCaseParser fileParser = new DATCFileParser();  // Will grab from "src/testgames/" directory by default

        Collection<TestCase> testCases = fileParser.parseMany();
        manager.testCases.addAll(new ArrayList<>(testCases));
        System.out.println("\n----------------------------------------\n");
        for (TestCase testCase : manager.testCases) {
            //  TODO: Variable output when un-commenting below line indicates...
            //      1. `Referee.java` is non-functional: FIX!! (easy)
            //      2. Underlying `Judge.java` algorithm is inconsistent: RESEARCH & FIX!! (hard)
            //      3. Some non-paradox test cases fail depending on order of orders: IMPROVE CATEGORIES!!
            //testCase.shuffle();
            testCase.eval(manager.willPrint());
        }

        System.out.println("----------------------------------------\n");
        for (TestCase testCase : manager.testCases) {
            testCase.printNameAndScore();
            if (testCase.getScore() != testCase.getSize())
                System.out.println(Constants.ANSI_RED + "\tFAILED!!" + Constants.ANSI_RESET);  // red color ANSI code (then black)
        }

        System.out.println("\n----------------------------------------");
        System.out.printf("TOTAL SCORE (by Test Cases):\t[%d/%d]\n", manager.score(), manager.size());
        System.out.printf("TOTAL SCORE (by Orders):\t\t[%d/%d]\n", manager.ordersScore(), manager.ordersSize());
        System.out.println("----------------------------------------\n");

        if (USE_REFEREE) {

            System.out.println();
            manager.testCases.clear();
            manager.testCases.addAll(new ArrayList<>(testCases));

            for (TestCase testCase : manager.testCases) {

                for (Order order : testCase.getOrders())
                    order.wipeMetaInf();

                int k = testCase.getOrders().size();
                try {
                    Referee referee = new Referee(testCase);
                    referee.judge();
                    if (manager.willPrint()) {
                        int p = referee.resolutions.size();
                        String ANSI1, ANSI2;
                        if (p > 1) {
                            ANSI1 = Constants.ANSI_YELLOW;
                            ANSI2 = Constants.ANSI_ORANGE;
                        } else {
                            ANSI1 = Constants.ANSI_BRIGHTWHITE;
                            ANSI2 = Constants.ANSI_RESET;
                        }
                        System.out.printf("%s[P=%d]%s (k=%d) \t%s%s%s\n",
                                ANSI1, p, ANSI2,
                                k,
                                TestCase.TESTCASE_PREFIX, testCase.getName(),
                                Constants.ANSI_RESET);
                    }
                } catch (TooIntensiveException ex) {
                    if (manager.willPrint())
                        System.out.printf("%s[P=?] (k=%d) \t%s%s\n\t%s%s%s\n",
                                Constants.ANSI_YELLOW, k,
                                TestCase.TESTCASE_PREFIX, testCase.getName(),
                                Constants.ANSI_RED, ex.getMessage(), Constants.ANSI_RESET);
                    else
                        System.err.printf("[P=?] (k=%d) \t%s%s\n\t%s\n",
                                k, TestCase.TESTCASE_PREFIX, testCase.getName(), ex.getMessage());
                }

            }

            Constants.printTimestamp();

        }

        // Blah

        Constants.printTimestamp();


    }


}