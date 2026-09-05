import java.util.*;

/**
 * `Referee` is a subclass of `Judge` which resolves both simple & complex
 * Paradoxes.<br><br>
 *
 * It does so by generating a large number of permutations, and running
 * `super::judge()` for them all to compare their results.<br><br>
 *
 * If there are multiple ( >1 ) possible resolutions (i.e. depends on
 * permutation), `Referee` will apply certain meta-resolution rules to
 * determine the correct resolution.
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
     * This intentionally does not influence normal candidate discovery.
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
     * Definitively meta-resolves the Collection of Orders `orders`, and applies
     * paradox-handling rules.
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
        List<Order> originalOrders = new ArrayList<>(
                Orders.deepCopy(this.orders)
        );

        originalOrders.sort(new OrderComparator());

        Random random = new Random(this.shuffleSeed);

        for (int trial = 1; trial <= this.numTrials; trial++) {

            List<Order> ordersClone = new ArrayList<>(
                    Orders.deepCopy(originalOrders)
            );

            Collections.shuffle(ordersClone, random);

            /*
             * Preserve the order before Judge mutates resolution metadata or
             * applies a Szykman convoy replacement.
             */
            List<Order> trialInputOrder = new ArrayList<>(
                    Orders.deepCopy(ordersClone)
            );

            this.orders = ordersClone;
            super.judge();

            Set<Order> outcome = new LinkedHashSet<>(
                    Orders.deepCopy(this.orders)
            );

            String outcomeKey = resolutionKey(outcome);

            this.resolutions.putIfAbsent(outcomeKey, outcome);

            CandidateObservation observation =
                    this.candidateObservations.computeIfAbsent(
                            outcomeKey,
                            ignored -> new CandidateObservation(
                                    outcome,
                                    trialInputOrder,
                                    super.getDetectedParadoxCycles()
                            )
                    );

            observation.recordTrial(trial);
            observation.recordDetectedCycles(
                    super.getDetectedParadoxCycles()
            );
            observation.recordResolutionStates(outcome);
        }

        /*
         * Restore the original submitted state before selecting a final
         * meta-resolution.
         */
        this.orders = new ArrayList<>(Orders.deepCopy(originalOrders));

        if (this.resolutions.size() == 1) {

            Set<Order> decisiveResolution =
                    this.resolutions.values().iterator().next();

            this.orders = new ArrayList<>(
                    Orders.deepCopy(decisiveResolution)
            );

        } else if (this.resolutions.size() > 1) {

            Set<Order> szykmanHolds = new HashSet<>();
            Set<Order> firstSzykmanSet = null;

            for (Set<Order> resolution : this.resolutions.values()) {
                for (Order order : resolution) {
                    if (order.getSnapshot() != null) {
                        szykmanHolds.add(order);

                        if (firstSzykmanSet == null)
                            firstSzykmanSet = resolution;
                    }
                }
            }

            Collection<Order> heuristicOrders = new HashSet<>(szykmanHolds);

            /*
             * S = total number of Szykman replacement holds over all
             * discovered outcomes.
             */
            int S = szykmanHolds.size();

            if (S == 0) {

                Set<Order> mostResolvedPerm = new HashSet<>();
                List<Set<Order>> otherMostResolvedPerms = new ArrayList<>();
                int mostNumResolved = -1;
                boolean tie = false;

                for (Set<Order> resolution : this.resolutions.values()) {

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

                    Map<String, Order> tiedConflictingConvoys =
                            this.findConflictingConvoys(otherMostResolvedPerms);

                    /*
                     * E11 reaches a tie in the number of resolved orders, but it has no
                     * conflicting convoy. Re-adjudicating an arbitrarily ordered Set makes
                     * the outcome depend on collection iteration order.
                     *
                     * In a non-convoy tie, prefer the candidate with the most successful
                     * orders and do not manufacture a Szykman scenario.
                     */
                    if (tiedConflictingConvoys.isEmpty()) {
                        mostResolvedPerm = this.selectMostSuccessfulCandidate(
                                otherMostResolvedPerms
                        );

                    } else {
                        /*
                         * A genuine convoy contradiction exists among the tied candidates.
                         * Preserve the legacy meta-Szykman approach for those cases.
                         */
                        Collection<Order> szykmanOrders =
                                this.szykmanRule(otherMostResolvedPerms);

                        this.orders = new ArrayList<>(
                                Orders.deepCopy(szykmanOrders)
                        );

                        for (Order order : this.orders)
                            order.wipeMetaInf();

                        super.judge();

                        mostResolvedPerm = new LinkedHashSet<>(
                                Orders.deepCopy(this.orders)
                        );
                    }

                }

                /*
                 * Narrow one-convoy paradox rule.
                 *
                 * F17 produces one contradictory convoy, and the selected
                 * highest-resolution candidate already fails both the convoy
                 * and its corresponding army move. However, it incorrectly
                 * permits an attack that dislodges the convoying fleet.
                 *
                 * If that direct attack itself remained unresolved in at least
                 * one raw candidate, it is part of the same ambiguous convoy
                 * dependency and cannot be accepted as a final success.
                 *
                 * This is intentionally limited to exactly one conflicting
                 * convoy. Multiple-convoy paradoxes remain handled by the
                 * existing meta-Szykman machinery below.
                 */
                mostResolvedPerm =
                        this.applySingleConvoyParadoxRule(mostResolvedPerm);

                heuristicOrders = mostResolvedPerm;

            } else if (S == 1) {

                heuristicOrders = firstSzykmanSet;

            } else {

                /*
                 * Retain the common non-Szykman orders from the first
                 * resolution, while preserving every discovered Szykman
                 * replacement hold.
                 */
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

            this.orders = new ArrayList<>(
                    Orders.deepCopy(heuristicOrders)
            );

        }

    }


    /**
     * Returns deep copies of every distinct raw outcome discovered before final
     * Referee meta-resolution selects a final result.
     */
    public Collection<Set<Order>> getCandidateResolutions() {

        Collection<Set<Order>> copies = new ArrayList<>();

        for (Set<Order> resolution : this.resolutions.values()) {
            copies.add(new LinkedHashSet<>(
                    Orders.deepCopy(resolution)));
        }

        return copies;

    }

    /**
     * Returns one immutable observation for every raw candidate resolution.<br><br>
     *
     * Observations include a representative outcome, occurrence count, trial
     * numbers, a representative shuffled input order, and detected dependency
     * cycles accumulated across matching trials.
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
        private final List<ParadoxCycle> detectedCycles;

        /*
         * `resolutionKey(...)` deliberately excludes `Order.resolved`, so
         * trials with the same external verdicts can have distinct recursive
         * bookkeeping. Retain all observed resolved states rather than making
         * the first representative outcome decide later meta-resolution.
         */
        private final Map<String, ResolutionStateRange> resolutionStates;

        private int occurrences;

        private CandidateObservation(
                Collection<Order> representativeResolution,
                Collection<Order> exampleInputOrder,
                Collection<ParadoxCycle> detectedCycles
        ) {

            this.representativeResolution = new LinkedHashSet<>(
                    Orders.deepCopy(representativeResolution)
            );

            this.exampleInputOrder = new ArrayList<>(
                    Orders.deepCopy(exampleInputOrder)
            );

            this.trialNumbers = new ArrayList<>();
            this.detectedCycles = new ArrayList<>(detectedCycles);

            this.resolutionStates = new LinkedHashMap<>();
            this.recordResolutionStates(representativeResolution);

            this.occurrences = 0;

        }

        private CandidateObservation(CandidateObservation other)
        {
            this.representativeResolution = new LinkedHashSet<>(
                    Orders.deepCopy(other.representativeResolution)
            );

            this.exampleInputOrder = new ArrayList<>(
                    Orders.deepCopy(other.exampleInputOrder)
            );

            this.trialNumbers = new ArrayList<>(other.trialNumbers);
            this.detectedCycles = new ArrayList<>(other.detectedCycles);

            this.resolutionStates = new LinkedHashMap<>();

            for (Map.Entry<String, ResolutionStateRange> entry :
                    other.resolutionStates.entrySet()) {
                this.resolutionStates.put(
                        entry.getKey(),
                        new ResolutionStateRange(entry.getValue())
                );
            }

            this.occurrences = other.occurrences;

        }

        private void recordTrial(int trial) {
            this.occurrences++;
            this.trialNumbers.add(trial);
        }

        private void recordDetectedCycles(Collection<ParadoxCycle> cycles) {
            for (ParadoxCycle cycle : cycles) {
                boolean alreadyKnown = false;
                for (ParadoxCycle existingCycle : this.detectedCycles) {
                    if (existingCycle.key().equals(cycle.key())) {
                        alreadyKnown = true;
                        break;
                    }
                }
                if (!alreadyKnown)
                    this.detectedCycles.add(cycle);
            }
        }

        /**
         * Records all resolver bookkeeping states observed for a raw,
         * verdict-level candidate. Snapshot-backed Szykman holds use their
         * original convoy identity so they remain associated with the
         * submitted convoy order.
         */
        private void recordResolutionStates(Collection<Order> outcome) {
            for (Order order : outcome) {
                String key = orderIdentityKey(originalOrderOf(order));
                this.resolutionStates.computeIfAbsent(
                        key,
                        ignored -> new ResolutionStateRange()
                ).observe(order.resolved);
            }
        }

        private boolean hasObservedResolvedState(Order order, boolean resolved) {
            ResolutionStateRange states = this.resolutionStates.get(
                    orderIdentityKey(originalOrderOf(order)));
            return states != null && states.hasObserved(resolved);
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

        public List<ParadoxCycle> getDetectedCycles() {
            return Collections.unmodifiableList(
                    new ArrayList<>(this.detectedCycles));
        }

        /**
         * Range of recursive bookkeeping states observed for one submitted
         * order under the same external verdict-level candidate key.
         */
        private static final class ResolutionStateRange {

            private boolean observedResolved;
            private boolean observedUnresolved;

            private ResolutionStateRange() {
                this.observedResolved = false;
                this.observedUnresolved = false;
            }

            private ResolutionStateRange(ResolutionStateRange other) {
                this.observedResolved = other.observedResolved;
                this.observedUnresolved = other.observedUnresolved;
            }

            private void observe(boolean resolved) {
                if (resolved)
                    this.observedResolved = true;
                else
                    this.observedUnresolved = true;
            }

            private boolean hasObserved(boolean resolved) {
                return resolved
                        ? this.observedResolved
                        : this.observedUnresolved;
            }

        }

    }


    /**
     * Deterministically selects the most successful candidate from candidates
     * already tied on the number of resolved orders.
     *
     * A lexicographically ordered resolution key is used only as a final stable
     * tie-breaker. This prevents HashSet iteration order from deciding a final
     * Referee outcome.
     */
    private Set<Order> selectMostSuccessfulCandidate(
            Collection<Set<Order>> candidates
    ) {

        Set<Order> selected = null;
        int highestSuccessfulCount = -1;
        String selectedKey = null;

        for (Set<Order> candidate : candidates) {

            int successfulCount = 0;

            for (Order order : candidate) {
                if (order.verdict)
                    successfulCount++;
            }

            String candidateKey = resolutionKey(candidate);

            if (selected == null
                    || successfulCount > highestSuccessfulCount
                    || (successfulCount == highestSuccessfulCount
                    && candidateKey.compareTo(selectedKey) < 0)) {

                selected = candidate;
                highestSuccessfulCount = successfulCount;
                selectedKey = candidateKey;
            }
        }

        return selected;
    }

    /**
     * Applies the narrow one-conflicting-convoy resolution policy.<br><br>
     *
     * The policy is intentionally conservative:<br><br>
     *
     * 1. There must be exactly one conflicting convoy.
     * 2. The selected candidate must already resolve that convoy as failed.
     * 3. The associated convoyed army must already fail in the selected result.
     * 4. A successful move directly attacking the convoy fleet must have an
     *    unstable `resolved` state across raw candidates.<br><br>
     *
     * A matching move satisfying all conditions is forced to fail. This retains
     * the selected candidate's result for every unrelated order.
     */
    private Set<Order> applySingleConvoyParadoxRule(Set<Order> selectedResolution) {

        Map<String, Order> conflictingConvoys =
                this.findConflictingConvoys(this.resolutions.values());

        if (conflictingConvoys.size() != 1)
            return selectedResolution;

        Order conflictingConvoy =
                conflictingConvoys.values().iterator().next();

        Order selectedConvoy = findMatchingOriginalOrder(
                conflictingConvoy,
                selectedResolution
        );

        if (selectedConvoy == null
                || selectedConvoy.verdict) {
            return selectedResolution;
        }

        Order correspondingMove = Orders.locateCorresponding(
                conflictingConvoy,
                selectedResolution
        );

        if (correspondingMove == null
                || correspondingMove.verdict) {
            return selectedResolution;
        }

        Set<Order> adjustedResolution = new LinkedHashSet<>(
                Orders.deepCopy(selectedResolution)
        );

        for (Order order : adjustedResolution) {

            if (order.orderType != OrderType.MOVE)
                continue;

            /*
             * Only direct attacks on the paradoxical convoy fleet are in scope.
             */
            if (!Province.equalsIgnoreCoast(
                    order.pos1,
                    conflictingConvoy.pos0
            )) {
                continue;
            }

            /*
             * The convoyed army is not attacking the convoy fleet's province
             * in the F17 shape, but exclude it explicitly for safety.
             */
            if (sameOriginalOrder(order, correspondingMove))
                continue;

            /*
             * A normal direct attack remains untouched. Only one whose
             * resolution bookkeeping differs across raw candidates belongs to
             * the contradictory convoy dependency.
             */
            if (!this.resolvedStateVariesAcrossCandidates(order))
                continue;

            order.resolved = true;
            order.verdict = false;
        }

        return adjustedResolution;
    }

    /**
     * Returns true if a submitted order's resolved/unresolved state differs
     * across every raw trial collected for every verdict-level candidate.<br><br>
     *
     * `resolutionKey(...)` intentionally ignores `resolved`; therefore this
     * method must use CandidateObservation's aggregate state rather than one
     * arbitrary first-discovered representative stored in `resolutions`.
     */
    private boolean resolvedStateVariesAcrossCandidates(Order order) {

        boolean observedResolved = false;
        boolean observedUnresolved = false;

        for (CandidateObservation observation :
                this.candidateObservations.values()) {

            if (observation.hasObservedResolvedState(order, true))
                observedResolved = true;

            if (observation.hasObservedResolvedState(order, false))
                observedUnresolved = true;

            if (observedResolved && observedUnresolved)
                return true;
        }

        return false;
    }

    /**
     * Finds submitted convoy orders whose adjudication differs across raw
     * candidate resolutions.<br><br>
     *
     * A transformed Szykman HOLD is treated as its original convoy by using
     * its retained snapshot.
     */
    private Map<String, Order> findConflictingConvoys(Collection<Set<Order>> resolutions) {

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

        return conflictingConvoys;

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

    private static Order findMatchingOriginalOrder(Order candidate, Collection<Order> resolution) {

        for (Order order : resolution) {
            if (sameOriginalOrder(candidate, order))
                return order;
        }

        return null;

    }

    private static Collection<Order> convoyOrdersIncludingSzykmanHolds(Collection<Order> resolution) {

        Collection<Order> convoyOrders = new ArrayList<>();

        for (Order order : resolution) {
            if (originalOrderOf(order).orderType == OrderType.CONVOY)
                convoyOrders.add(order);
        }

        return convoyOrders;

    }


    /**
     * Handles paradoxical situations involving conflicting convoy outcomes by
     * replacing each conflicting convoy with a snapshot-backed HOLD.
     * (This is a 'programmatic hold')<br><br>
     *
     * Returns a new collection and does not mutate the candidate resolutions.<br><br>
     *
     * This existing helper is used only in the historical tie-handling branch.
     * The F17 single-convoy rule above deliberately does not call this method,
     * because a transformed HOLD is not itself the final disrupted-convoy
     * result.
     */
    private Collection<Order> szykmanRule(Collection<Set<Order>> resolutions) {

        if (resolutions.isEmpty())
            return Collections.emptyList();

        Map<String, Order> conflictingConvoys =
                this.findConflictingConvoys(resolutions);

        /*
         * Begin with a representative resolution. The replacements below
         * overwrite only orders identified as conflicting convoys.
         */
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

        /*
         * Replace each genuinely conflicting convoy by a HOLD while preserving
         * the original convoy in the replacement order's snapshot.
         */
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