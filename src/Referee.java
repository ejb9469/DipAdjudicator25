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


    public static final int NUM_TRIALS_DEFAULT = 300;  // good range: [300 <-> 2500]


    private final int numTrials;
    protected final Collection<Set<Order>> resolutions;


    public Referee() {
        super();
        this.resolutions = new HashSet<>();
        this.numTrials = NUM_TRIALS_DEFAULT;
    }

    public Referee(Collection<Order> orders) {
        super(orders);
        this.resolutions = new HashSet<>();
        this.numTrials = NUM_TRIALS_DEFAULT;
    }

    public Referee(int numTrials) {
        super();
        this.resolutions = new HashSet<>();
        this.numTrials = numTrials;
    }

    public Referee(Collection<Order> orders, int numTrials) {
        super(orders);
        this.resolutions = new HashSet<>();
        this.numTrials = numTrials;
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

        Collection<Order> originalOrders = new ArrayList<>(Orders.deepCopy(this.orders));

        List<Order> ordersClone;
        for (int trial = 1; trial <= this.numTrials; trial++) {

            // deep clone the orders
            ordersClone = new ArrayList<>(Orders.deepCopy(originalOrders));
            Collections.shuffle(ordersClone);  // generate a random permutation

            // evaluate
            this.orders = ordersClone;
            super.judge();
            // will only truly add to `resolutions` if the resolution is unique,
            // ... because we are using a Set [equality determined by `Order::hashcode()`]
            resolutions.add(new HashSet<>(Set.copyOf(
                    Orders.deepCopy(this.orders))));

        }

        // reset the judge
        this.orders = originalOrders;

        if (resolutions.size() == 1) {

            // set `this.orders` to the 1 and only resolution
            for (Set<Order> decisiveResolution : resolutions)  // (only runs once; size() == 1)
                this.orders = decisiveResolution;

        } else if (resolutions.size() > 1) {

           /* The current "A-B-C" REFEREE RULES for multiple resolutions / 'paradoxes' -
                1. Compound all replacement orders into one set where all Szykman holds are present
                2. The remaining orders will be taken from:
                        A) EITHER the merged Szykman set                                    (if "S">1)
                        B) OR     the (1 and only) Szykman set                              (if S=1)
                        C) OR     the set with the most # of Orders with `resolved=true`    (if S=0)
                                    ...if there is a tie, call the 'meta-Szykman' procedure: `Referee::szykmanRule(...)`
                                        AND call `super::judge()` fresh!
            */

            // add all szykman holds from all permutations / resolutions
            Set<Order> szykmanHolds = new HashSet<>();
            Set<Order> firstSzykmanSet = null;
            for (Set<Order> resolution : resolutions) {
                for (Order order : resolution) {
                    if (order.getSnapshot() != null) {
                        szykmanHolds.add(order);
                        if (firstSzykmanSet == null)
                            firstSzykmanSet = resolution;
                    }
                }
            }

            // build out the remaining orders from here
            Collection<Order> heuristicOrders = new HashSet<>(szykmanHolds);

            // `S` ==> the # of total Szykman Holds over all resolutions
            int S = szykmanHolds.size();
            if (S == 0) {

                // find order set with most `resolved=true`
                Set<Order>      mostResolvedPerm        = new HashSet<>();  // blank instead of null
                Set<Set<Order>> otherMostResolvedPerms  = new HashSet<>();
                int             mostNumResolved         = -1;
                boolean         tie                     = false;

                for (Set<Order> resolution : resolutions) {
                    int numResolved = 0;
                    for (Order order : resolution)
                        numResolved += (order.resolved ? 1 : 0);  // 1 if true, 0 if false (ternary operator)
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

                    /* It seems that we (only) arrive here in three (3) different multi-layered paradox Test Case situations:
                        1. 6.E.11. NO SELF DISLODGEMENT WITH BELEAGUERED GARRISON,
                                    UNIT SWAP WITH ADJACENT CONVOYING AND TWO COASTS
                        2. 6.F.18. BETRAYAL PARADOX
                        3. 6.F.21. DAD'S ARMY CONVOY                                */
                    // In all cases, another 'layered' / 'meta-' Szykman backup rule should be applied!

                    otherMostResolvedPerms.add(mostResolvedPerm);
                    mostResolvedPerm = Set.copyOf(this.szykmanRule(otherMostResolvedPerms));
                    this.orders = new ArrayList<>(List.copyOf(Orders.deepCopy(mostResolvedPerm)));
                    for (Order order : this.orders)
                        order.wipeMetaInf();
                    super.judge();
                    mostResolvedPerm = Set.copyOf(this.orders);

                }

                // populate the new order set with the winner
                heuristicOrders = mostResolvedPerm;


            } else if (S == 1) {

                heuristicOrders = firstSzykmanSet;


            } else /*if (S>1)*/ {

                for (Order order : firstSzykmanSet) {
                    boolean found = false;
                    for (Order holdOrder : szykmanHolds) {
                        if (holdOrder.pos0 == order.pos0) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        heuristicOrders.add(order);
                }

            }

            // assign to `this.orders`
            this.orders = heuristicOrders;

        }

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

        Collection<Order> verdict           = new HashSet<>();
        Collection<Order> ordersToReplace   = new HashSet<>();

        // Determine all Convoy orders with differing resolutions,
        // ==> add them all to `ordersToReplace` (wysiwyg)
        Collection<Order> convoyOrders;  // declare up here to save heap space
        for (Set<Order> resolution : resolutions) {

            convoyOrders = Orders.pruneForOrderType(OrderType.CONVOY, resolution);  // & assign down here
            for (Order convoyOrder : convoyOrders) {

                boolean diffResolution = false;  // will be `true` if metadata is equal, `false` otherwise

                Collection<Order> convoyOrders2;
                for (Set<Order> resolution2 : resolutions) {
                    if (resolution == resolution2) continue;
                    convoyOrders2 = Orders.pruneForOrderType(OrderType.CONVOY, resolution2);
                    for (Order convoyOrder2 : convoyOrders2) {
                        if (convoyOrder.equals(convoyOrder2)) {
                            // `diffResolution` ==> `true` if metadata is equal, `false` otherwise
                            diffResolution = (convoyOrder.hashCode() != convoyOrder2.hashCode());
                            break;
                        }
                    }
                }

                // differing resolution for `convoyOrder`: add (CLONE) to `ordersToReplace`
                if (diffResolution) {
                    ordersToReplace.add(new Order(convoyOrder));  // clone
                    break;
                }

            }

        }

        // apply Szykman Rule -- replace problem-convoys in `ordersToReplace` with holds and add to `verdict`
        for (Order order : ordersToReplace) {

            // take snapshot (will transform into Szykman hold)
            order.takeSnapshot();

            order.pos1 = null;
            order.pos2 = null;
            order.orderType = OrderType.HOLD;

            boolean found = false;
            for (Order verdictOrder : verdict) {
                if (order.pos0 == verdictOrder.pos0) {
                    found = true;
                    break;
                }
            }
            if (!found)
                verdict.add(order);

        }

        // all other orders (CLONES) are added to `verdict`,
        // ... [arbitrarily] from the FIRST resolution =>  \\ (see comment below!) //  <=
        for (Set<Order> resolution : resolutions) {  // this for "loop" will run only once
            for (Order order : resolution) {
                boolean found = false;
                for (Order verdictOrder : verdict) {
                    if (order.pos0 == verdictOrder.pos0) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    verdict.add(new Order(order));  // clone
            }
            break;  // this for "loop" will run only once
        }

        return verdict;  // should be entirely made up of cloned Orders; no 'originals'

    }


}