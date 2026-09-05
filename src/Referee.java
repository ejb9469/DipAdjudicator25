import java.util.*;

/**
 * `Referee` is a subclass of `Judge` which resolves both simple & complex Paradoxes.<br><br>
 *
 * It does so by generating a large number of permutations, and running `super::judge()` for them all to compare their results.<br><br>
 *
 * If there are multiple ( >1 ) possible resolutions (i.e. depends on permutation), ...<br>
 *      ...`Referee` will apply certain 'meta-resolution' rules to determine the correct resolution.
 *
 * @author Evan B
 */
public class Referee extends Judge {


    public static final int NUM_TRIALS_DEFAULT = 300;

    /*
     * A fixed default makes a test run reproducible. Supply a different seed
     * through the three-argument constructor when investigating instability.
     */
    public static final long SHUFFLE_SEED_DEFAULT = 0xD1A10C4CL;

    private final int numTrials;
    private final long shuffleSeed;

    /*
     * Key: canonical externally meaningful adjudication outcome.
     * Value: representative deep-copied resolution for that key.
     */
    protected final Map<String, Set<Order>> resolutions;

    /*
     * Additional diagnostic metadata for each raw candidate resolution.
     *
     * This intentionally does not influence final meta-resolution logic.
     */
    private final Map<String, CandidateObservation> candidateObservations;


    public Referee() {
        this(Collections.emptyList(), NUM_TRIALS_DEFAULT, SHUFFLE_SEED_DEFAULT);
    }

    public Referee(Collection<Order> orders) {
        this(orders, NUM_TRIALS_DEFAULT, SHUFFLE_SEED_DEFAULT);
    }

    public Referee(int numTrials) {
        this(Collections.emptyList(), numTrials, SHUFFLE_SEED_DEFAULT);
    }

    public Referee(Collection<Order> orders, int numTrials) {
        this(orders, numTrials, SHUFFLE_SEED_DEFAULT);
    }

    /**
     * Creates a Referee with a known seed, allowing reproducible trial ordering.
     *
     * @param orders orders to adjudicate
     * @param numTrials number of shuffled orderings to examine; must be at least 1
     * @param shuffleSeed seed used to generate shuffled orderings
     */
    public Referee(Collection<Order> orders, int numTrials, long shuffleSeed) {

        super(orders);

        if (numTrials < 1)
            throw new IllegalArgumentException("numTrials must be at least 1");

        /*
         * TreeMap gives every discovered candidate a canonical iteration order.
         */
        this.resolutions = new TreeMap<>();
        this.candidateObservations = new TreeMap<>();

        this.numTrials = numTrials;
        this.shuffleSeed = shuffleSeed;

    }


    /**
     * Definitively meta-resolves the Collection of Orders `orders`, and applies Paradox-handling rules.
     *
     * @author Evan B
     */
    @Override
    public void judge() {

        this.resolutions.clear();
        this.candidateObservations.clear();

        /*
         * Begin from a stable order before shuffling. This ensures that the same
         * input order set plus the same seed yields the same sequence of trials.
         */
        List<Order> originalOrders = new ArrayList<>(Orders.deepCopy(this.orders));
        originalOrders.sort(new OrderComparator());

        Random random = new Random(this.shuffleSeed);

        for (int trial = 1; trial <= this.numTrials; trial++) {

            List<Order> ordersClone = new ArrayList<>(Orders.deepCopy(originalOrders));
            Collections.shuffle(ordersClone, random);

            /*
             * Preserve the order before Judge mutates resolution metadata or
             * applies a Szykman convoy replacement.
             */
            List<Order> trialInputOrder =
                    new ArrayList<>(Orders.deepCopy(ordersClone));

            this.orders = ordersClone;
            super.judge();

            Set<Order> outcome = new LinkedHashSet<>(
                    Orders.deepCopy(this.orders)
            );

            String outcomeKey = resolutionKey(outcome);

            this.resolutions.putIfAbsent(outcomeKey, outcome);

            this.candidateObservations.computeIfAbsent(
                    outcomeKey,
                    ignored -> new CandidateObservation(
                            outcome,
                            trialInputOrder
                    )
            ).recordTrial(trial);
        }

        // Restore the original input state before selecting the final resolution.
        this.orders = new ArrayList<>(Orders.deepCopy(originalOrders));

        if (resolutions.size() == 1) {

            Set<Order> decisiveResolution =
                    this.resolutions.values().iterator().next();

            this.orders = new ArrayList<>(
                    Orders.deepCopy(decisiveResolution)
            );

        } else if (resolutions.size() > 1) {

            Set<Order> szykmanHolds = new HashSet<>();
            Set<Order> firstSzykmanSet = null;

            for (Set<Order> resolution : resolutions.values()) {
                for (Order order : resolution) {
                    if (order.getSnapshot() != null) {
                        szykmanHolds.add(order);

                        if (firstSzykmanSet == null)
                            firstSzykmanSet = resolution;
                    }
                }
            }

            Collection<Order> heuristicOrders = new HashSet<>(szykmanHolds);

            // S = total number of Szykman holds over all discovered outcomes.
            int S = szykmanHolds.size();

            if (S == 0) {

                Set<Order> mostResolvedPerm = new HashSet<>();
                List<Set<Order>> otherMostResolvedPerms = new ArrayList<>();
                int mostNumResolved = -1;
                boolean tie = false;

                for (Set<Order> resolution : resolutions.values()) {

                    int numResolved = 0;

                    for (Order order : resolution)
                        numResolved += order.resolved ? 1 : 0;

                    if (mostResolvedPerm.isEmpty()
                            || numResolved > mostNumResolved) {
                        mostResolvedPerm = resolution;
                        mostNumResolved = numResolved;
                        tie = false;
                        otherMostResolvedPerms.clear();

                    } else if (numResolved == mostNumResolved) {
                        tie = true;
                        otherMostResolvedPerms.add(resolution);
                    }
                }

                if (tie) {
                    otherMostResolvedPerms.add(mostResolvedPerm);

                    mostResolvedPerm = Set.copyOf(
                            this.szykmanRule(otherMostResolvedPerms)
                    );

                    this.orders = new ArrayList<>(
                            Orders.deepCopy(mostResolvedPerm)
                    );

                    for (Order order : this.orders)
                        order.wipeMetaInf();

                    super.judge();

                    mostResolvedPerm = Set.copyOf(this.orders);
                }

                heuristicOrders = mostResolvedPerm;

            } else if (S == 1) {

                heuristicOrders = firstSzykmanSet;

            } else {

                for (Order order : firstSzykmanSet) {

                    boolean foundSzykmanHoldAtPosition = false;

                    for (Order holdOrder : szykmanHolds) {
                        if (holdOrder.pos0 == order.pos0) {
                            foundSzykmanHoldAtPosition = true;
                            break;
                        }
                    }

                    if (!foundSzykmanHoldAtPosition)
                        heuristicOrders.add(order);
                }
            }

            this.orders = new ArrayList<>(Orders.deepCopy(heuristicOrders));
        }
    }


    /**
     * Returns deep copies of every distinct raw outcome discovered before final
     * Referee meta-resolution selects a final result.
     */
    public Collection<Set<Order>> getCandidateResolutions() {

        Collection<Set<Order>> copies = new ArrayList<>();

        for (Set<Order> resolution : this.resolutions.values()) {
            copies.add(new LinkedHashSet<>(Orders.deepCopy(resolution)));
        }

        return copies;
    }

    /**
     * Returns one immutable observation for every raw candidate resolution.
     *
     * Observations include a representative outcome, occurrence count, trial
     * numbers, and one example shuffled input order that produced the outcome.
     */
    public Collection<CandidateObservation> getCandidateObservations() {

        Collection<CandidateObservation> copies = new ArrayList<>();

        for (CandidateObservation observation :
                this.candidateObservations.values()) {
            copies.add(new CandidateObservation(observation));
        }

        return copies;
    }


    /**
     * Diagnostic information for one distinct raw Judge outcome observed during
     * this Referee instance's shuffled trials.
     */
    public static final class CandidateObservation {

        private final Set<Order> representativeResolution;
        private final List<Order> exampleInputOrder;
        private final List<Integer> trialNumbers;
        private int occurrences;

        private CandidateObservation(
                Collection<Order> representativeResolution,
                Collection<Order> exampleInputOrder
        ) {
            this.representativeResolution = new LinkedHashSet<>(
                    Orders.deepCopy(representativeResolution)
            );

            this.exampleInputOrder = new ArrayList<>(
                    Orders.deepCopy(exampleInputOrder)
            );

            this.trialNumbers = new ArrayList<>();
            this.occurrences = 0;
        }

        private CandidateObservation(CandidateObservation other) {
            this.representativeResolution = new LinkedHashSet<>(
                    Orders.deepCopy(other.representativeResolution)
            );

            this.exampleInputOrder = new ArrayList<>(
                    Orders.deepCopy(other.exampleInputOrder)
            );

            this.trialNumbers = new ArrayList<>(other.trialNumbers);
            this.occurrences = other.occurrences;
        }

        private void recordTrial(int trial) {
            this.occurrences++;
            this.trialNumbers.add(trial);
        }

        public int getOccurrences() {
            return this.occurrences;
        }

        public List<Integer> getTrialNumbers() {
            return Collections.unmodifiableList(
                    new ArrayList<>(this.trialNumbers)
            );
        }

        public Set<Order> getRepresentativeResolution() {
            return new LinkedHashSet<>(
                    Orders.deepCopy(this.representativeResolution)
            );
        }

        public List<Order> getExampleInputOrder() {
            return new ArrayList<>(
                    Orders.deepCopy(this.exampleInputOrder)
            );
        }
    }


    private static String resolutionKey(Collection<Order> resolution) {

        List<String> entries = new ArrayList<>();

        for (Order order : resolution) {
            entries.add(
                    orderIdentityKey(order)
                            + "\u001Fverdict=" + order.verdict
                            + "\u001Fsnapshot=" + snapshotIdentityKey(order)
            );
        }

        Collections.sort(entries);
        return String.join("\n", entries);

    }

    private static String orderIdentityKey(Order order) {

        return String.valueOf(order.owner)
                + "\u001F" + String.valueOf(order.unitType)
                + "\u001F" + String.valueOf(order.orderType)
                + "\u001F" + String.valueOf(order.pos0)
                + "\u001F" + String.valueOf(order.pos1)
                + "\u001F" + String.valueOf(order.pos2)
                + "\u001F" + order.dislodged;

    }

    private static String snapshotIdentityKey(Order order) {

        Order snapshot = order.getSnapshot();

        if (snapshot == null)
            return "<none>";

        return orderIdentityKey(snapshot);

    }

    private static Order originalOrderOf(Order order) {

        Order snapshot = order.getSnapshot();

        if (snapshot != null)
            return snapshot;

        return order;

    }

    private static boolean sameOriginalOrder(Order first, Order second) {
        return originalOrderOf(first).equals(originalOrderOf(second));
    }

    private static boolean sameAdjudicationOutcome(Order first, Order second) {

        return first.resolved == second.resolved
                && first.verdict == second.verdict
                && (first.getSnapshot() != null)
                == (second.getSnapshot() != null);

    }

    private static Order findMatchingOriginalOrder(
            Order candidate,
            Collection<Order> resolution
    ) {

        for (Order order : resolution) {
            if (sameOriginalOrder(candidate, order))
                return order;
        }

        return null;

    }

    private static Collection<Order> convoyOrdersIncludingSzykmanHolds(
            Collection<Order> resolution
    ) {

        Collection<Order> convoyOrders = new ArrayList<>();

        for (Order order : resolution) {
            if (originalOrderOf(order).orderType == OrderType.CONVOY)
                convoyOrders.add(order);
        }

        return convoyOrders;

    }


    /**
     * Applies the Szykman rule at Referee meta-resolution level.
     */
    private Collection<Order> szykmanRule(Collection<Set<Order>> resolutions) {

        if (resolutions.isEmpty())
            return Collections.emptyList();

        Map<String, Order> conflictingConvoys = new LinkedHashMap<>();

        for (Set<Order> resolution : resolutions) {
            for (Order convoyOrder :
                    convoyOrdersIncludingSzykmanHolds(resolution)) {

                boolean differsAcrossResolutions = false;

                for (Set<Order> otherResolution : resolutions) {

                    Order matchingOrder = findMatchingOriginalOrder(
                            convoyOrder,
                            otherResolution
                    );

                    if (matchingOrder == null
                            || !sameAdjudicationOutcome(
                            convoyOrder,
                            matchingOrder
                    )) {
                        differsAcrossResolutions = true;
                        break;
                    }
                }

                if (differsAcrossResolutions) {
                    Order originalConvoy = new Order(
                            originalOrderOf(convoyOrder)
                    );

                    conflictingConvoys.putIfAbsent(
                            orderIdentityKey(originalConvoy),
                            originalConvoy
                    );
                }
            }
        }

        Set<Order> representativeResolution =
                resolutions.iterator().next();

        Collection<Order> verdict = new LinkedHashSet<>();

        for (Order order : representativeResolution) {

            Order originalOrder = originalOrderOf(order);
            String originalKey = orderIdentityKey(originalOrder);

            if (conflictingConvoys.containsKey(originalKey))
                continue;

            verdict.add(new Order(order));
        }

        for (Order originalConvoy : conflictingConvoys.values()) {

            Order szykmanHold = new Order(originalConvoy);

            szykmanHold.takeSnapshot();
            szykmanHold.orderType = OrderType.HOLD;
            szykmanHold.pos1 = null;
            szykmanHold.pos2 = null;

            verdict.add(szykmanHold);
        }

        return verdict;

    }

}