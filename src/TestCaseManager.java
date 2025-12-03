import java.util.*;

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

        Constants.printTimestamp();

        TestCaseManager manager = new TestCaseManager(true);
        FileTestCaseParser fileParser = new DATCFileParser();  // Will grab from "src/testgames/" directory by default

        Collection<TestCase> testCases = fileParser.parseMany();
        manager.testCases.addAll(new ArrayList<>(testCases));
        System.out.println("\n----------------------------------------\n");

        // REFEREE MODE //
        // TODO: Make new `Referee.java` using below code as base
        int NUM_TRIALS  = 2500;
        Map<TestCase, Collection<Set<Order>>> refereeSimul = new HashMap<>();
        Collection<Set<Order>> permutations;
        for (TestCase testCase : manager.testCases) {
            permutations = new HashSet<>();
            for (int i = 1; i <= NUM_TRIALS; i++) {
                // (deep) clone the testcase + its orders:
                // ... twice as fast to do this vs. using 1 test case for all trials, but terribly large heap store
                TestCase testCaseClone = new TestCase(testCase);
                testCaseClone.shuffle();  // generate a random permutation
                testCaseClone.eval();     // evaluate the testcase
                // will only truly add to `permutations` if the resolution is unique,
                // ... because we are using a Set [equality determined by `Order::hashcode()`]
                permutations.add(new HashSet<>(Set.copyOf(
                        Orders.deepCopy(testCaseClone.getOrders()))));
            }
            refereeSimul.put(testCase, permutations);
        }

        System.out.println("REFEREE SIMUL TESTING:\n");
        for (TestCase testCase : refereeSimul.keySet()) {
            System.out.printf("[P=%d]\t%s\n", refereeSimul.get(testCase).size(), testCase.getName());
        }

        System.out.println("----------------------------------------\n");
        System.out.println("ONE-OFF TESTING:\n");
        for (TestCase testCase : manager.testCases) {
            //testCase.shuffle();  // uncomment for randomly-ordered Order List
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

        // Blah //

        Constants.printTimestamp();


    }


}