import java.util.*;

public class TestCaseManager {


    // MODE 0: `Referee.java` implementation
    // MODE 1: pre-Referee implementation
    public static final short MODE = 0;


    protected final List<TestCase> testCases;
    protected final boolean prints;


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

        Collection<TestCase> testCases = fileParser.parseManyFiles();
        manager.testCases.addAll(new ArrayList<>(testCases));
        System.out.println("\n----------------------------------------\n");

        switch (MODE) {

            case 0 -> {

                // REFEREE MODE //
                System.out.println("REFEREE ONE-OFF TESTING:\n");
                List<TestCaseReferee> testCaseRefs = new ArrayList<>();
                for (TestCase testCase : manager.testCases) {
                    TestCaseReferee testCaseRef = new TestCaseReferee(testCase);
                    testCaseRefs.add(testCaseRef);
                    //testCaseRef.shuffle();  // uncomment for randomly-ordered Order List
                    testCaseRef.eval(manager.willPrint());
                }

                System.out.println("----------------------------------------\n");
                for (TestCaseReferee testCase : testCaseRefs) {
                    testCase.printNameAndScore();
                    if (testCase.getScore() != testCase.getSize())
                        System.out.println(Constants.ANSI_RED + "\tFAILED!!" + Constants.ANSI_RESET);  // red color ANSI code (then black)
                }

                manager.testCases.clear();
                manager.testCases.addAll(testCaseRefs);

                System.out.println("\n----------------------------------------");
                System.out.printf("TOTAL SCORE (by Test Cases):\t[%d/%d]\n", manager.score(), manager.size());
                System.out.printf("TOTAL SCORE (by Orders):\t\t[%d/%d]\n", manager.ordersScore(), manager.ordersSize());
                System.out.println("----------------------------------------\n");

            }

            case 1 -> {

                // QUASI-REFEREE + JUDGE MODE //
                int NUM_TRIALS  = Referee.NUM_TRIALS_DEFAULT;
                Map<TestCase, Collection<Set<Order>>> refereeSimul = new HashMap<>(manager.testCases.size());
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

                System.out.println("\n----------------------------------------");
                System.out.println("REFEREE SIMUL TESTING - PARADOX CASES:\n");
                Map<TestCase, Collection<Set<Order>>> refereeSimulParadoxes = new HashMap<>();
                for (TestCase testCase : refereeSimul.keySet()) {
                    if (refereeSimul.get(testCase).size() > 1) {
                        refereeSimulParadoxes.put(testCase, refereeSimul.get(testCase));
                        System.out.printf("[P=%d]\t%s\n", refereeSimul.get(testCase).size(), testCase.getName());
                    }
                }
                System.out.printf("\nTOTAL # PARADOXES: [%d]\n", refereeSimulParadoxes.size());
                // END //

                System.out.println("----------------------------------------\n");
                Referee ref;
                for (TestCase paradox : refereeSimulParadoxes.keySet()) {
                    ref = new Referee(paradox.getOrders());
                    ref.judge();
                    System.out.println(paradox.getName());
                    for (Order order : ref.getOrders())
                        System.out.println("\t" + order.toString() + ":\n\t\t" + order.metaToString());
                    System.out.println();
                }


                System.out.println("----------------------------------------\n");
                System.out.println("ONE-OFF (Judge) TESTING:\n");
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

            }

        }

        Constants.printTimestamp();

    }


}