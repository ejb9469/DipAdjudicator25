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
     * Key:     canonical full adjudication outcome, including resolver metadata.
     * Value:   a representative deep-copied resolution for that key.
     *
     * `Order.equals()` intentionally ignores mutable metadata, so `Set<Order>`
     * equality cannot be used to determine whether two adjudication outcomes
     * differ only by `verdict`/`resolved` state.
     */
    protected final Map<String, Set<Order>> resolutions;


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

        //this.resolutions = new LinkedHashMap<>();
        /*
         * `TreeMap` (!!) gives every discovered resolution a canonical iteration order.
         *
         * This does not decide which Diplomacy rule is correct, but it prevents
         * random shuffle/discovery order from deciding which candidate is considered
         * "first" by the meta-resolution implementation.
         */
        this.resolutions = new TreeMap<>();
        this.numTrials = numTrials;
        this.shuffleSeed = shuffleSeed;

    }


    /**
     * Definitively meta-resolves the Collection of Orders `orders`, and applies Paradox-handling rules.<br><br>
     *
     * <b>Will call `Judge::judge()` for many (10^kn) permutations,
     * and determine all unique resolutions to the same Order Set.</b><br><br>
     *
     * ~ In the event there is only <i>1 resolution</i> to `orders`, it will simply pass it through.<br>
     * ~ In the event there are <i>multiple resolutions</i> to `orders`,
     * `Referee` will attempt to 'meta-resolve': i.e. apply Paradox-handling rules.<br><br>
     *
     * The most notable Paradox-handling rule is the <i>Szykman Rule</i> / Principle, which is applied in layers, both here and in base `Judge`.
     *
     * @author Evan B
     */
    @Override
    public void judge() {

        /*
         * Referee instances may be reused. Do not mix outcomes from an earlier
         * call to judge() with outcomes from the current order set.
         */
        this.resolutions.clear();

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

            this.orders = ordersClone;
            super.judge();

            Set<Order> outcome = new HashSet<>(Orders.deepCopy(this.orders));
            this.resolutions.putIfAbsent(resolutionKey(outcome), outcome);
        }

        // Restore the original input state before selecting the final resolution.
        this.orders = new ArrayList<>(Orders.deepCopy(originalOrders));

        if (resolutions.size() == 1) {

            Set<Order> decisiveResolution = this.resolutions.values().iterator().next();
            this.orders = new ArrayList<>(Orders.deepCopy(decisiveResolution));

        } else if (resolutions.size() > 1) {

            /*
             * Current A-B-C meta-resolution rules:
             *
             * 1. Merge all Szykman replacement holds.
             * 2. Select remaining orders from:
             *      A) merged Szykman holds, if there is more than one;
             *      B) the sole Szykman-containing resolution, if there is one; or
             *      C) the resolution with the most resolved orders, if there are none.
             * 3. If rule C ties, use the meta-Szykman procedure and adjudicate again.
             */

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

                    if (mostResolvedPerm.isEmpty() || numResolved > mostNumResolved) {
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

                /*
                 * Retain the common non-Szykman orders from the first resolution,
                 * while preserving every discovered Szykman replacement hold.
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
     * Creates a stable key for the externally meaningful result of a complete
     * adjudication outcome.
     *
     * "resolved" is intentionally excluded: it is recursive-algorithm bookkeeping,
     * not a Diplomacy result. Two outcomes that differ only in that flag must be
     * treated as the same resolution.
     *
     * The key includes:
     * - the final order identity;
     * - success/failure (verdict); and
     * - the original convoy identity, when a convoy was replaced by a Szykman hold.
     */
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

    /**
     * Produces a stable representation of an order's identity fields.
     */
    private static String orderIdentityKey(Order order) {

        return String.valueOf(order.owner)
                + "\u001F" + String.valueOf(order.unitType)
                + "\u001F" + String.valueOf(order.orderType)
                + "\u001F" + String.valueOf(order.pos0)
                + "\u001F" + String.valueOf(order.pos1)
                + "\u001F" + String.valueOf(order.pos2)
                + "\u001F" + order.dislodged;

    }

    /**
     * Includes the original convoy order when this order was replaced by a
     * Szykman hold. This prevents a normal HOLD from being conflated with a
     * transformed convoy order.
     */
    private static String snapshotIdentityKey(Order order) {

        Order snapshot = order.getSnapshot();

        if (snapshot == null)
            return "<none>";

        return orderIdentityKey(snapshot);

    }

    /**
     * Returns the originally submitted order.
     *
     * A Szykman replacement hold retains the original convoy order in its
     * snapshot. Ordinary orders have no snapshot and are returned directly.
     */
    private static Order originalOrderOf(Order order) {

        Order snapshot = order.getSnapshot();

        if (snapshot != null)
            return snapshot;

        return order;

    }

    /**
     * Returns whether two resolved orders originated from the same submitted order.
     *
     * This correctly treats a Szykman replacement HOLD and its original CONVOY
     * as the same underlying order.
     */
    private static boolean sameOriginalOrder(Order first, Order second) {
        return originalOrderOf(first).equals(originalOrderOf(second));
    }

    /**
     * Returns whether two versions of an order have the same adjudication result.
     *
     * This must not use Order.hashCode(). hashCode() intentionally ignores mutable
     * adjudication metadata so Orders remain safe as HashSet / HashMap keys.
     */
    private static boolean sameAdjudicationOutcome(Order first, Order second) {

        return first.resolved == second.resolved
                && first.verdict == second.verdict
                && (first.getSnapshot() != null) == (second.getSnapshot() != null);

    }

    /**
     * Finds the version of candidate's original order inside a candidate outcome.
     */
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

    /**
     * Returns all orders whose original submitted type was CONVOY.
     *
     * This includes ordinary convoy orders and Szykman-transformed HOLD orders.
     */
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
     * Handles paradoxical situations re: differing-resolution Convoys by replacing the offending Convoy orders with Holds.<br><br>
     *
     * <i><u>Returns</u> a <u>new</u>  <u>Set</u> of <u>CLONED</u> Orders.</i><br>
     * <i><u>Does not mutate</u> any data!</i><br>
     * --> (unlike `Judge::szykmanRule()`)<br><br>
     *
     * Applies the Szykman Rule / Principle at a 'meta' level; this method is the "meta-Szykman function".<br><br>
     *
     * Szykman Rule / Principle definition: "All Convoy orders in the paradoxical convoy situation are forced to hold"
     *
     * @param resolutions Collection of resolution Order Sets to parse through
     * @return A new Set of Orders based on `resolutions`, with the Szykman Rule applied to all problem convoys
     *
     * @author Evan B
     */
    private Collection<Order> szykmanRule(Collection<Set<Order>> resolutions) {

        if (resolutions.isEmpty())
            return Collections.emptyList();

        /*
         * Maps every conflicting original convoy to one clean copy of its original
         * submitted order. `LinkedHashMap` preserves discovery order for diagnostics.
         */
        Map<String, Order> conflictingConvoys = new LinkedHashMap<>();

        /*
         * A convoy is conflicting if its outcome differs across candidate
         * resolutions. This comparison includes:
         *
         * - `resolved`;
         * - `verdict`; and
         * - whether it was replaced with a Szykman hold.
         *
         * It deliberately does NOT use `Order.hashCode()`.
         */
        for (Set<Order> resolution : resolutions) {
            for (Order convoyOrder : convoyOrdersIncludingSzykmanHolds(resolution)) {

                boolean differsAcrossResolutions = false;

                for (Set<Order> otherResolution : resolutions) {

                    Order matchingOrder = findMatchingOriginalOrder(
                            convoyOrder,
                            otherResolution
                    );

                    if (matchingOrder == null
                            || !sameAdjudicationOutcome(convoyOrder, matchingOrder)) {
                        differsAcrossResolutions = true;
                        break;
                    }
                }

                if (differsAcrossResolutions) {
                    Order originalConvoy = new Order(originalOrderOf(convoyOrder));

                    conflictingConvoys.putIfAbsent(
                            orderIdentityKey(originalConvoy),
                            originalConvoy
                    );
                }
            }
        }

        /*
         * Begin with a representative resolution. The Szykman replacements below
         * overwrite only orders identified as conflicting.
         */
        Set<Order> representativeResolution = resolutions.iterator().next();

        Collection<Order> verdict = new LinkedHashSet<>();

        for (Order order : representativeResolution) {

            Order originalOrder = originalOrderOf(order);
            String originalKey = orderIdentityKey(originalOrder);

            if (conflictingConvoys.containsKey(originalKey)) {
                continue;
            }

            verdict.add(new Order(order));
        }

        /*
         * Replace each actually conflicting convoy by a HOLD, retaining the
         * original convoy as its snapshot.
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