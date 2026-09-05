import java.util.*;

public class TestCaseManager {


    // MODE 0: `Referee.java` implementation
    // MODE 1: pre-Referee implementation
    public static final short MODE = 0;

    /*
     * Limit printed provenance samples per candidate. The complete candidate
     * frequency remains available through occurrences and seed counts.
     */
    private static final int MAX_PROVENANCE_SAMPLES = 12;


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


    public void addTestCaseWithFields(
            TestCase testCase,
            boolean evalNow,
            boolean... expectedFields
    ) {
        testCase.setExpectedFields(expectedFields);
        this.testCases.add(testCase);

        if (evalNow)
            testCase.eval(this.prints);
    }

    public void addTestCaseWithFields(
            TestCase testCase,
            boolean evalNow,
            boolean[]... expectedFields
    ) {
        testCase.setExpectedFields(expectedFields);
        this.testCases.add(testCase);

        if (evalNow)
            testCase.eval(this.prints);
    }


    public static void main(String[] args) {

        /* Random sleep (?)
            try {
                Thread.sleep(new Random().nextInt(250, 1250));
            } catch (InterruptedException ex) {}
         */
        System.out.println();
        Constants.printTimestamp();

        TestCaseManager manager = new TestCaseManager(true);
        FileTestCaseParser fileParser = new DATCFileParser();

        Collection<TestCase> testCases = fileParser.parseManyFiles();
        manager.testCases.addAll(new ArrayList<>(testCases));

        /*
         * Temporary diagnostics for all currently unresolved / unstable cases.
         *
         * Remove this invocation after the investigation, but keep
         * diagnoseRefereeStability(...) for future regressions.
         */
        for (TestCase testCase : manager.testCases) {
            String name = testCase.getName();

            if (//name.contains("6.E.11")
                    /*||*/ name.contains("6.F.17.P")
                    //|| name.contains("6.F.23.P")
                    //|| name.contains("6.F.24.P")) {
            ) {
                diagnoseRefereeStability(
                        testCase,
                        50,
                        Referee.NUM_TRIALS_DEFAULT
                );
            }
        }

        System.out.println("\n----------------------------------------\n");

        switch (MODE) {

            case 0 -> {

                System.out.println("REFEREE ONE-OFF TESTING:\n");

                List<TestCaseReferee> testCaseRefs = new ArrayList<>();

                for (TestCase testCase : manager.testCases) {
                    TestCaseReferee testCaseRef =
                            new TestCaseReferee(testCase);

                    testCaseRefs.add(testCaseRef);
                    testCaseRef.eval(manager.willPrint());
                }

                System.out.println("----------------------------------------\n");

                for (TestCaseReferee testCase : testCaseRefs) {
                    testCase.printNameAndScore();

                    if (testCase.getScore() != testCase.getSize()) {
                        System.out.println(
                                Constants.ANSI_RED
                                        + "\tFAILED!!"
                                        + Constants.ANSI_RESET
                        );
                    }
                }

                manager.testCases.clear();
                manager.testCases.addAll(testCaseRefs);

                System.out.println("\n----------------------------------------");
                System.out.printf(
                        "TOTAL SCORE (by Test Cases):\t[%d/%d]%n",
                        manager.score(),
                        manager.size()
                );
                System.out.printf(
                        "TOTAL SCORE (by Orders):\t\t[%d/%d]%n",
                        manager.ordersScore(),
                        manager.ordersSize()
                );
                System.out.println("----------------------------------------\n");

            }

            case 1 -> {

                int NUM_TRIALS = Referee.NUM_TRIALS_DEFAULT;

                Map<TestCase, Collection<Set<Order>>> refereeSimul =
                        new HashMap<>(manager.testCases.size());

                Collection<Set<Order>> permutations;

                for (TestCase testCase : manager.testCases) {
                    permutations = new HashSet<>();

                    for (int i = 1; i <= NUM_TRIALS; i++) {
                        TestCase testCaseClone = new TestCase(testCase);
                        testCaseClone.shuffle();
                        testCaseClone.eval();

                        permutations.add(new HashSet<>(Set.copyOf(
                                Orders.deepCopy(testCaseClone.getOrders())
                        )));
                    }

                    refereeSimul.put(testCase, permutations);
                }

                System.out.println("REFEREE SIMUL TESTING:\n");

                for (TestCase testCase : refereeSimul.keySet()) {
                    System.out.printf(
                            "[P=%d]\t%s%n",
                            refereeSimul.get(testCase).size(),
                            testCase.getName()
                    );
                }

                System.out.println("\n----------------------------------------");
                System.out.println(
                        "REFEREE SIMUL TESTING - PARADOX CASES:\n"
                );

                Map<TestCase, Collection<Set<Order>>>
                        refereeSimulParadoxes = new HashMap<>();

                for (TestCase testCase : refereeSimul.keySet()) {
                    if (refereeSimul.get(testCase).size() > 1) {
                        refereeSimulParadoxes.put(
                                testCase,
                                refereeSimul.get(testCase)
                        );

                        System.out.printf(
                                "[P=%d]\t%s%n",
                                refereeSimul.get(testCase).size(),
                                testCase.getName()
                        );
                    }
                }

                System.out.printf(
                        "%nTOTAL # PARADOXES: [%d]%n",
                        refereeSimulParadoxes.size()
                );

                System.out.println("----------------------------------------\n");

                Referee ref;

                for (TestCase paradox : refereeSimulParadoxes.keySet()) {
                    ref = new Referee(paradox.getOrders());
                    ref.judge();

                    System.out.println(paradox.getName());

                    for (Order order : ref.getOrders()) {
                        System.out.println(
                                "\t" + order + ":\n\t\t"
                                        + order.metaToString()
                        );
                    }

                    System.out.println();
                }

                System.out.println("----------------------------------------\n");
                System.out.println("ONE-OFF (Judge) TESTING:\n");

                for (TestCase testCase : manager.testCases)
                    testCase.eval(manager.willPrint());

                System.out.println("----------------------------------------\n");

                for (TestCase testCase : manager.testCases) {
                    testCase.printNameAndScore();

                    if (testCase.getScore() != testCase.getSize()) {
                        System.out.println(
                                Constants.ANSI_RED
                                        + "\tFAILED!!"
                                        + Constants.ANSI_RESET
                        );
                    }
                }

                System.out.println("\n----------------------------------------");
                System.out.printf(
                        "TOTAL SCORE (by Test Cases):\t[%d/%d]%n",
                        manager.score(),
                        manager.size()
                );
                System.out.printf(
                        "TOTAL SCORE (by Orders):\t\t[%d/%d]%n",
                        manager.ordersScore(),
                        manager.ordersSize()
                );
                System.out.println("----------------------------------------\n");

            }
        }

        Constants.printTimestamp();

    }


    /**
     * Runs a test case repeatedly with known seeds and reports:
     *
     * - distinct final Referee outcomes;
     * - distinct raw Judge candidates;
     * - raw-candidate occurrence frequency;
     * - seeds and trial samples that produced each candidate; and
     * - one example shuffled input order for each candidate.
     */
    public static void diagnoseRefereeStability(
            TestCase testCase,
            int numSeeds,
            int numTrials
    ) {

        Set<String> finalOutcomes = new TreeSet<>();

        Map<String, CandidateStats> candidateStats = new TreeMap<>();

        for (long seed = 0; seed < numSeeds; seed++) {

            Referee referee = new Referee(
                    new ArrayList<>(Orders.deepCopy(testCase.getOrders())),
                    numTrials,
                    seed
            );

            referee.judge();

            finalOutcomes.add(finalOutcomeKey(referee.getOrders()));

            for (Referee.CandidateObservation observation :
                    referee.getCandidateObservations()) {

                String candidateKey = outcomeKey(
                        observation.getRepresentativeResolution()
                );

                CandidateStats stats = candidateStats.computeIfAbsent(
                        candidateKey,
                        ignored -> new CandidateStats(
                                observation.getExampleInputOrder()
                        )
                );

                stats.record(
                        seed,
                        observation.getOccurrences(),
                        observation.getTrialNumbers()
                );
                stats.recordCycles(observation.getDetectedCycles());
            }
        }

        System.out.printf("%n[%s]%n", testCase.getName());

        System.out.printf(
                "Observed %d final outcome(s) across %d seed(s), %d trial(s) per seed.%n",
                finalOutcomes.size(),
                numSeeds,
                numTrials
        );

        System.out.printf(
                "Observed %d raw candidate resolution(s) before final meta-resolution.%n",
                candidateStats.size()
        );

        int candidateNumber = 1;

        for (Map.Entry<String, CandidateStats> entry :
                candidateStats.entrySet()) {

            CandidateStats stats = entry.getValue();

            System.out.printf(
                    "%n--- RAW CANDIDATE %d ---%n",
                    candidateNumber++
            );

            System.out.printf(
                    "Observed %d time(s) across %d seed(s).%n",
                    stats.occurrences,
                    stats.seeds.size()
            );

            System.out.printf(
                    "Seeds: %s%n",
                    stats.seeds
            );

            if (!stats.provenanceSamples.isEmpty()) {
                System.out.printf(
                        "Trial samples: %s%n",
                        stats.provenanceSamples
                );
            }

            System.out.println("Example shuffled input order:");

            for (int i = 0; i < stats.exampleInputOrder.size(); i++) {
                System.out.printf(
                        "  [%d] %s%n",
                        i,
                        stats.exampleInputOrder.get(i)
                );
            }

            stats.printCycles();

            System.out.printf("%n%s%n", entry.getKey());
        }

        int finalNumber = 1;

        for (String finalOutcome : finalOutcomes) {
            System.out.printf(
                    "%n--- FINAL OUTCOME %d ---%n%s%n",
                    finalNumber++,
                    finalOutcome
            );
        }
    }


    private static String finalOutcomeKey(Collection<Order> orders) {
        return outcomeKey(orders);
    }

    private static String outcomeKey(Collection<Order> orders) {

        List<String> lines = new ArrayList<>();

        for (Order order : orders) {
            lines.add(
                    order
                            + " | resolved=" + order.resolved
                            + " | verdict=" + order.verdict
                            + " | snapshot="
                            + (order.getSnapshot() != null)
            );
        }

        Collections.sort(lines);

        return String.join("\n", lines);
    }


    private static final class CandidateStats {

        private int occurrences;
        private final Set<Long> seeds;
        private final List<String> provenanceSamples;
        private final List<Order> exampleInputOrder;
        private final Map<String, ParadoxCycle> detectedCycles;

        private CandidateStats(Collection<Order> exampleInputOrder) {
            this.occurrences = 0;
            this.seeds = new TreeSet<>();
            this.provenanceSamples = new ArrayList<>();
            this.exampleInputOrder = new ArrayList<>(
                    Orders.deepCopy(exampleInputOrder)
            );
            this.detectedCycles = new TreeMap<>();
        }

        private void record(
                long seed,
                int occurrencesForSeed,
                Collection<Integer> trialNumbers
        ) {
            this.occurrences += occurrencesForSeed;
            this.seeds.add(seed);
            for (int trial : trialNumbers) {
                if (this.provenanceSamples.size()
                        >= MAX_PROVENANCE_SAMPLES) {
                    return;
                }
                this.provenanceSamples.add(
                        "seed=" + seed + ", trial=" + trial
                );
            }
        }

        private void recordCycles(Collection<ParadoxCycle> cycles) {
            for (ParadoxCycle cycle : cycles) {
                this.detectedCycles.putIfAbsent(cycle.key(), cycle);
            }
        }

        private void printCycles() {
            if (this.detectedCycles.isEmpty()) {
                System.out.println("Detected convoy/dependency cycles: none");
                return;
            }

            System.out.printf(
                    "Detected convoy/dependency cycles: %d%n",
                    this.detectedCycles.size()
            );

            int number = 1;

            for (ParadoxCycle cycle : this.detectedCycles.values()) {
                System.out.printf(
                        "  -- CYCLE %d --%n%s%n",
                        number++,
                        cycle
                );
            }
        }

    }

}