import java.util.*;

/**
 * The `Judge` class holds a Collection of Orders, and contains the Adjudication & Resolution logic required to definitively process them all in sequence:
 * see `Judge.judge(...)`<br><br>
 *
 * Utilizes a duplex recursive algorithm, where `resolve(...)` handles dependency logic i.e. <i>"resolution via deduction"</i>,
 * and `adjudicate(...)` handles board logic i.e. <i>"resolution via force"</i>
 */
public class Judge {

    // The adjudication program needs to handle the following situations:
        // a. An order that is not indirectly dependent on itself
        // b. An order that is indirectly dependent on itself, but there is still exactly 1 resolution
        // c. An order that is indirectly dependent on itself, but there are 0 or 2 possible resolutions

    protected final Collection<Order> orders;


    public Judge() {
        this.orders = new ArrayList<>();
    }

    public Judge(List<Order> orders) {
        this.orders = orders;
    }


    public Collection<Order> getOrders() {
        return orders;
    }


    /**
     * <u>Global vars for the `resolve()` func:</u><br>
     *      ~ <i>(List of Orders)</i> `<i><b>cycle</b></i>` contains the contents of a recursion cycle, if it exists (empty otherwise)<br>
     *      ~ <i>int</i> `<i><b>recursionHits</b></i>` represents the cyclic dependency depth<br>
     *      ~ <i>bool</i> `<i><b>uncertain</b></i>` is the "guessing variable" --
     *          when true, indicates resolve() returns a result based on uncertain information
     *          ... (i.e. is guessing)
     */
    private List<Order> cycle = new ArrayList<>();
    private int         recursionHits = 0;
    private boolean     uncertain = false;


    /**
     * <i><u>Definitively</u></i> resolves the Collection of Orders `orders`.<br><br>
     *
     * Acquires Orders' resolution 'verdicts' by calling top-level `resolve(...)` 2x per Order:<br>
     *      ~ 1st Mass-Resolve: sets each `order.verdict` to the output of the call `resolve(order, optimistic=true)`<br>
     *      ~ 2nd Mass-Resolve: does not directly set `order.verdict`, but still calls `resolve(order, optimistic=true)` for each order
     *
     * @postcondition Every order in `orders` is definitively resolved and has a verdict<br>
     *                (Note: This should be enough information to infer dislodgement status)
     *
     * @author Evan B
     */
    public void judge() {

        // 1st run
        for (Order order : orders)
            order.verdict = resolve(order, true);

        // 2nd run
        for (Order order : orders)
            resolve(order, true);

    }

    /**
     * Performs the necessary adjudication equations to resolve an Order.<br><br>
     *
     * Does not know anything about Order states;<br>
     * Instead, calls `resolve(order2, ...)` to determine whether an Order succeeds or fails.<br>
     * These calls may return guess-based results, but this is of no concern to the `adjudicate` function.
     *
     * @param order Order to adjudicate
     * @param optimistic Caller's `optimistic` bool -- which `adjudicate` may sometimes flip (for "opponents") in subsequent calls
     * @return True if `order` is logistically successful, potentially based on `resolve(...)` guesswork -- false otherwise
     *
     * @author Evan B
     */
    private boolean adjudicate(Order order, boolean optimistic) {

        // Handle MOVE orders
        if (order.orderType == OrderType.MOVE) {

            int attackStrength;

            Order headToHead = Orders.locateHeadToHead(order, this.orders);
            if (headToHead != null) {  // HEAD-TO-HEAD Battle

                // Calculate Move order's ATTACK STRENGTH
                // [Must be greater than... a. the Defend Strength of the opposing mover, and
                //                          b. the Prevent Strength of all movers competing for the same area]
                attackStrength = calculateAttackStrength(order, optimistic, true, orders);

                // Calculate opponent's DEFEND STRENGTH
                int opponentDefendStrength = calculateDefendStrength(headToHead, optimistic, orders);

                Collection<Order> otherOpponents = Orders.locateUnitsMovingToPosition(order.pos1, orders);

                if (attackStrength > opponentDefendStrength) {

                    // Move is completely unopposed
                    if (otherOpponents.size() <= 1)
                        return true;

                    // Calculate PREVENT STRENGTH of all 'opponents' (other movers going to the same destination)
                    // returns true if our Move order is the greatest (with no ties)
                    return champion(order, attackStrength, optimistic, otherOpponents);

                } else if (Orders.adjacentMatchingConvoyFleetExists(order, orders) &&
                        Orders.adjacentMatchingConvoyFleetExists(headToHead, orders)) {
                    // Lost to opponent mover, the move will fail unless there are Convoy-Swap hijinx

                    // Test for Convoy-Swaps (very specific edge case)
                    // Convoy-Swaps will succeed if-and-only-if:
                    //      1) 'This' move is successful if re-evaluated as a NON-HEAD-TO-HEAD Battle; i.e. ...
                    //              ... a) The ATTACK STRENGTH is greater than the Hold Strength of the area...
                    //                  b) ...and the Prevent Strength of all movers competing for the same area
                    //      2) The 'other' move is also successful -- i.e. it also satisfies condition #1
                    boolean otherMoveSuccessful = resolve(headToHead, optimistic);
                    int destHoldStrength = calculateHoldStrength(order.pos1, optimistic, orders);

                    return otherMoveSuccessful &&
                            (attackStrength > destHoldStrength) &&
                            champion(order, attackStrength, optimistic, otherOpponents);

                } else {  // Cannot overwhelm nor swap with the Head-to-Head adversary
                    return false;
                }

            } else {  // NON-HEAD-TO-HEAD Battle

                // Calculate Move order's ATTACK STRENGTH
                // [Must be greater than... a. the Hold Strength of the area, and
                //                          b. the Prevent Strength of all movers competing for the same area]
                attackStrength = calculateAttackStrength(order, optimistic, false, orders);

                // Calculate destination's HOLD STRENGTH
                int destHoldStrength = calculateHoldStrength(order.pos1, optimistic, orders);

                Collection<Order> otherOpponents = Orders.locateUnitsMovingToPosition(order.pos1, orders);

                if (attackStrength > destHoldStrength) {

                    // Calculate PREVENT STRENGTH of all 'opponents' (other movers going to the same destination)
                    // returns true if our Move order is the greatest (with no ties)
                    return champion(order, attackStrength, optimistic, otherOpponents);

                } else {  // Lost on hold strength, return false
                    return false;
                }

            }

        }

        // Handle SUPPORT orders
        else if (order.orderType == OrderType.SUPPORT) {

            // Supports will fail without a corresponding order
            if (Orders.locateCorresponding(order, orders) == null)
                return false;

            for (Order order2 : orders) {

                if (order2.equals(order) || order2.orderType != OrderType.MOVE)
                    continue;

                if (order2.pos1 != order.pos0)
                    continue;

                if (pathSuccessful(order2, optimistic, orders) &&
                        order2.owner != order.owner &&
                        order.pos2 != order2.pos0) {
                    return false;
                } else if (resolve(order2, !optimistic)) {
                    return false;
                }

            }

            return true;

        }

        // Handle CONVOYS
        else if (order.orderType == OrderType.CONVOY) {

            // Convoys will fail without a corresponding order
            if (Orders.locateCorresponding(order, orders) == null)
                return false;

            Collection<Order> assailants = Orders.locateUnitsMovingToPosition(order.pos0, orders);
            for (Order assailant : assailants) {
                if (assailant.equals(order)) continue;
                if (resolve(assailant, !optimistic)) {

                    Order matchingMoveOrder = Orders.locateCorresponding(order, this.orders);
                    if (matchingMoveOrder != null)
                        if (!matchingMoveOrder.pos0.isAdjacentTo(matchingMoveOrder.pos1)) {
                            // There exists a Move to & from non-adjacent squares that matches this convoy's specifications,
                            // and this convoy is now dislodged
                            // Therefore, the move cannot possibly go through
                            // This is a 'shortcut' and violates the division of responsibility btwn. `adjudicate()` and `resolve()`
                            matchingMoveOrder.resolved = true;
                            matchingMoveOrder.verdict = false;
                        }

                    return false;

                }
            }

            return true;

        }

        // Handle HOLDS
        else if (order.orderType == OrderType.HOLD) {

            Collection<Order> assailants = Orders.locateUnitsMovingToPosition(order.pos0, orders);
            for (Order assailant : assailants) {
                if (assailant.equals(order)) continue;
                if (resolve(assailant, !optimistic))
                    return false;
            }

            return true;

        }

        // Handle RETREAT & PIFF orders (retreats phase)
        else if (order.orderType == OrderType.RETREAT) {

            // Retreated off the board; 'piffed'
            // technically successful order; SPECIAL CASE, will be handled at the unit removal level
            if (order.pos1 == null)
                return true;

            // The retreat fails if & only if there is another (dislodged) unit retreating there
            Collection<Order> bouncers = Orders.locateUnitsMovingToPosition(order.pos1, orders);
            return (bouncers.size() > 1);

        }

        // Unknown / impossible order type, throw exception
        System.err.println("LINE 158 :: Judge.java");
        throw new IllegalStateException("Impossible OrderType");

    }


    /**
     * Resolves an Order based on a (possibly cyclic) dependency chain, updating state information along the way.<br>
     * Return values of `resolve` are sometimes guess-based, thus any single run cannot be considered definitive by itself.<br><br>
     *
     * Determines result based on preexisting Order state information (`order.resolved` & `order.verdict`) and `optimistic`/pessimistic heuristic.<br>
     * Does not know anything about the underlying adjudication equations;<br>
     * Instead, calls `adjudicate(order, ...)` 1-2 times per Order to determine adjudication results.<br>
     * These calls may, in turn, call `resolve(order2, ...)` to determine the status of dependent orders.
     *
     * @param order Order to resolve
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of `order`
     * @return 'Best guess' as to the verdict of `order`
     *
     * @author Lucas B. Kruijswijk (<a href="https://webdiplomacy.net/doc/DATC_v3_0.html">...</a>,
     * <a href="https://diplom.org/Zine/S2009M/Kruijswijk/DipMath_Chp6.htm">...</a>)
     * @author revised by Evan B
     */
    private boolean resolve(Order order, boolean optimistic) {

        if (order.resolved)
            // Resolution already exists
            return order.verdict;


        if (cycle.contains(order)) {
            // We already concluded this order is in a cycle,
            // ... which we cannot yet resolve
            // Result is based on uncertain information
            uncertain = true;
            // Success if optimistic
            return optimistic;
        }

        if (order.visited) {
            // We hit cyclic dependency
            // Success if optimistic
            cycle.add(order);
            recursionHits++;
            uncertain = true;
            return optimistic;
        }

        order.visited = true;  // Prevent endless recursion; block from recursing to self
        int cycleLen_Old = cycle.size();
        int recursionHits_Old = recursionHits;
        boolean uncertain_Old = uncertain;
        uncertain = false;
        boolean optResult = this.adjudicate(order, true);
        boolean pesResult;
        // Try to avoid a 2nd adjudication for performance
        if (optResult && uncertain)
            pesResult = this.adjudicate(order, false);
        else
            pesResult = optResult;
        //pesResult = this.adjudicate(order, false);
        order.visited = false;  // Un-block recursion for this Order

        if (optResult == pesResult) {
            // We have a single resolution
            // Delete any cycle info that was found in recursion
            if (cycleLen_Old >= cycle.size())
                cycle.clear();
            else
                cycle.subList(0, cycleLen_Old).clear();
            recursionHits = recursionHits_Old;
            // The uncertain variable must be unaltered, because the order is now resolved
            uncertain = uncertain_Old;
            // Store the result and return it
            order.verdict = optResult;
            order.resolved = true;
            return optResult;
        }

        if (cycle.contains(order)) {
            // We returned from recursion, where this order hit the cycle,
            // ... and we didn't receive any resolution
            recursionHits--;
        }

        if (recursionHits == recursionHits_Old) {
            // We have sufficiently retreated from recursion such that ...
            // ... this order is the ancestor of the whole cycle
            // Apply the backup rule on all orders in the cycle
            this.backupRule(cycle.subList(cycleLen_Old, cycle.size()));
            cycle.subList(0, cycleLen_Old).clear();
            uncertain = uncertain_Old;
            // The backup rule may not have resolved THIS order
            return this.resolve(order, optimistic);
        } else {
            // We returned from a situation where a cycle was detected
            // However, this order is not the ancestor of the whole cycle
            // We further retreat from recursion
            if (!cycle.contains(order))
                cycle.add(order);
            return optimistic;
        }

    }

    /**
     * Subroutine of `resolve(...)`, handles cyclical Order dependencies<br><br>
     *
     * These dependencies may either be comprised of all Move Orders, in which case, all Orders are forced through as `resolved = true` and `verdict = true`,<br>
     * ... OR there are Convoy orders present in the chain, in which case, call the Szykman Rule (force all paradoxical Convoys to hold) subroutine.
     *
     * @param cyclicalOrders List of cyclic Order dependencies
     */
    private void backupRule(List<Order> cyclicalOrders) {

        boolean areAllMovers = true;
        for (Order order : cyclicalOrders) {
            if (order.orderType != OrderType.MOVE) {
                areAllMovers = false;
                break;
            }
        }

        if (areAllMovers) {
            for (Order order : cyclicalOrders) {
                order.resolved = true;
                order.verdict = true;
            }
        } else {
            szykmanRule(cyclicalOrders);
        }

    }

    /**
     * Subroutine of `backupRule(...)`, handles paradoxical Convoy situations by applying the Szykman Rule<br><br>
     *
     * Szykman Rule definition: "All Convoy orders in the paradoxical convoy situation are forced to hold"
     *
     * @param cyclicalOrders List of cyclic Order dependencies
     */
    private void szykmanRule(List<Order> cyclicalOrders) {

        for (Order order : cyclicalOrders) {

            if (order.orderType == OrderType.CONVOY) {
                order.pos1 = null;
                order.pos2 = null;
                order.orderType = OrderType.HOLD;
            }

        }

    }


    /**
     * Adjudication subroutine which returns true if a given Move Order can successfully reach its destination -- i.e. "has a successful path" -- false otherwise<br><br>
     *
     * A Move Order's PATH is successful if:<br>
     *      ~ 1) The Order is valid, <i>AND...</i><br>
     *      ~ 2A) <i>EITHER...</i> The unit is adjacent to its destination, there are no matching Convoy(s), and the unit can, theoretically, move to its destination >> [<b>LAND ROUTE</b>]<br>
     *      ~ 2B)     <i>OR...</i> The unit is an army, and there exist 1+ matching Convoy(s) / matching series(') of uninterrupted (successful) Convoys >> [<b>WATER ROUTE</b>]<br>
     *      ~ 2C)     <i>OR...</i> The unit is adjacent to its destination, is an army, there exist Convoy(s) / ...(ditto), but the 'Water Route' is NOT successful >> [<b>LAND ROUTE</b>]
     *
     * @param moveOrder Move Order whose path to test
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of `moveOrder`
     * @param orders Collection of all Orders to test against
     * @return Whether `moveOrder`'s path is successful; `moveOrder` touches its destination
     */
    private boolean pathSuccessful(Order moveOrder, boolean optimistic, Collection<Order> orders) {

        if (!Orders.orderIsValid(moveOrder))
            return false;

        boolean treatAsConvoyingArmy = (moveOrder.unitType == UnitType.ARMY &&
                Orders.adjacentMatchingConvoyFleetExists(moveOrder, orders));

        if (!treatAsConvoyingArmy) {
            return moveOrder.pos0.isAdjacentTo(moveOrder.pos1);  // Try land route, no convoys detected
        } else {

            // Try the first convoy route, and allow the path (return true) if every convoying fleet succeeds (i.e. is not dislodged)
            // If the first route is unsuccessful, begin looping for possible convoy routes

            Collection<Order> convoyOrders = Orders.pruneForOrderType(OrderType.CONVOY, orders);
            List<Order> convoyPath = Convoys.drawConvoyPath(moveOrder, convoyOrders);
            List<Order> unsuccessfulConvoys = new ArrayList<>();
            for (Order convoyOrder : convoyPath) {
                if (!resolve(convoyOrder, optimistic))
                    unsuccessfulConvoys.add(convoyOrder);
            }

            if (unsuccessfulConvoys.isEmpty())
                return true;
            else {

                // Multiple Convoy Routes:
                // Check if there is a different convoy route,
                // ... comprised of fleets that are all successful
                // Continue to check until `Convoys.drawConvoyPath()` returns an empty collection,
                // ... or we find a path of successful convoys

                convoyOrders.removeAll(unsuccessfulConvoys);
                convoyPath = Convoys.drawConvoyPath(moveOrder, convoyOrders);
                for ( ; !convoyPath.isEmpty(); convoyPath = Convoys.drawConvoyPath(moveOrder, convoyOrders)) {
                    unsuccessfulConvoys.clear();
                    for (Order convoyOrder : convoyPath) {
                        if (!resolve(convoyOrder, optimistic))
                            unsuccessfulConvoys.add(convoyOrder);
                    }
                    if (unsuccessfulConvoys.isEmpty())
                        return true;
                    else
                        convoyOrders.removeAll(unsuccessfulConvoys);
                }

                // No convoy path available -- only the land route
                return moveOrder.pos0.isAdjacentTo(moveOrder.pos1);

            }

        }

    }


    /**
     * Count & return the # of successful Support Orders attributed to a given Order<br><br>
     *
     * Support Orders are adjudicated as unsuccessful if they are 'cut' -- i.e. there exists a Move Order with a successful path that has 'touched' the support
     *
     * @param order Order whose support(s) to tally
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of `order`
     * @param orders Collection of Orders to search
     * @return # of successful Support Orders in `orders` attributed to `order`
     */
    private int tallySuccessfulSupports(Order order, boolean optimistic, Collection<Order> orders) {

        int supports = 0;
        if (order.orderType == OrderType.MOVE) {  // SUPPORT to MOVE

            for (Order order2 : orders) {

                // Invalid / illegal supports do not count
                if (!Orders.orderIsValid(order))
                    continue;

                if (order2.equals(order) || order2.orderType != OrderType.SUPPORT)
                    continue;

                if (order2.pos1 == order.pos0 && order2.pos2 == order.pos1) {
                    if (resolve(order2, optimistic))
                        supports++;
                }

            }

        } else {  // SUPPORT to HOLD

            for (Order order2 : orders) {
                if (order2.equals(order) || order2.orderType != OrderType.SUPPORT)
                    continue;

                if (order2.pos1 == order.pos0 && order2.pos2 == null) {
                    if (resolve(order2, optimistic))
                        supports++;
                }

            }

        }

        return supports;

    }

    /**
     * Count & return the # of successful Support Orders attributed to a given Order that are NOT coming from a given Nation<br><br>
     *
     * Identical to `tallySuccessfulSupports()`, but will not tally any supports originating from the Nation `forbiddenOwner`<br>
     * The most common use for this function is to ignore a Nation's own support for the purpose of avoiding self-dislodgement
     *
     * @param order Order whose support(s) to tally
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of `order`
     * @param forbiddenOwner Nation whose Support Orders to ignore
     * @param orders Collection of Orders to search
     * @return # of successful Support Orders in `orders` attributed to `order`, which are not coming from `forbiddenOwner`
     */
    private int tallySuccessfulSupportsForeign(Order order, boolean optimistic, Nation forbiddenOwner, Collection<Order> orders) {

        int supports = 0;
        if (order.orderType == OrderType.MOVE) {  // SUPPORT to MOVE

            for (Order order2 : orders) {

                // Invalid / illegal supports do not count
                if (!Orders.orderIsValid(order))
                    continue;

                if (order2.equals(order) || order2.orderType != OrderType.SUPPORT ||
                        order2.owner == forbiddenOwner)
                    continue;

                if (order2.pos1 == order.pos0 && order2.pos2 == order.pos1) {
                    if (resolve(order2, optimistic))
                        supports++;
                }

            }

        } else {  // SUPPORT to HOLD

            for (Order order2 : orders) {
                if (order2.equals(order) || order2.orderType != OrderType.SUPPORT ||
                        order2.owner == forbiddenOwner)
                    continue;

                if (order2.pos1 == order.pos0 && order2.pos2 == null) {
                    if (resolve(order2, optimistic))
                        supports++;
                }

            }

        }

        return supports;

    }

    /**
     * Calculate the <i>Prevent Strength</i> of all Movers going to the same destination as a given Move Order.<br>
     * Return whether the Move Order's <i>Attack Strength</i> is greater than the Prevent Strength of all other Movers to the same area ("opponents")
     *
     * @param moveOrder Move Order whose 'champion status' to determine
     * @param attackStrength Attack Strength of `moveOrder`: passed by caller, not re-calculated within
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of `moveOrder` and its opponents (inverted for opponents)
     * @param opponents Collection of opposing Movers ("opponents")
     * @return True if the Attack Strength of `moveOrder` is greater than the Prevent Strengths of all Move Orders in Collection `opponents` -- false otherwise
     */
    private boolean champion(Order moveOrder, int attackStrength, boolean optimistic, Collection<Order> opponents) {

        // Calculate PREVENT STRENGTH of all 'opponents' (other movers going to the same destination)
        // `champion` will be true if our Move order is the greatest (with no ties)
        boolean champion = true;

        for (Order order2 : opponents) {
            if (order2.equals(moveOrder)) continue;
            int opponentPreventStrength = calculatePreventStrength(order2, !optimistic, orders);
            if (opponentPreventStrength >= attackStrength) {
                champion = false;
                break;
            }
        }

        return champion;

    }


    /**
     * Calculate a Move Order's <i>Attack Strength</i><br><br>
     *
     * Attack Strength is defined as the strength (of a Move Order) to attack & conquer its destination Province
     *
     * @param moveOrder Move Order whose Attack Strength to calculate
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of `moveOrder` and its opponents (inverted for opponents)
     * @param headToHead Whether `moveOrder` is engaged in a Head-to-Head Battle (with another Move Order)
     * @param orders Collection of Orders to search
     * @return Attack Strength of `moveOrder`
     */
    private int calculateAttackStrength(Order moveOrder, boolean optimistic, boolean headToHead, Collection<Order> orders) {

        if (moveOrder.orderType != OrderType.MOVE)
            throw new IllegalArgumentException(String.format("Non-Move Order supplied for `calculateAttackStrength(...)`: %s", moveOrder));

        if (!pathSuccessful(moveOrder, optimistic, orders))
            return 0;

        if (Orders.locateUnitAtPosition(moveOrder.pos1, orders) == null)
            return 1+tallySuccessfulSupports(moveOrder, !optimistic, orders);

        Order destOrder = Orders.locateUnitAtPosition(moveOrder.pos1, orders);
        if (!headToHead) {
            if (resolve(destOrder, optimistic)) {
                return 1+tallySuccessfulSupports(moveOrder, !optimistic, orders);
            } else if (destOrder.owner == moveOrder.owner) {
                return 0;
            }
        }

        return 1+tallySuccessfulSupportsForeign(moveOrder, optimistic, destOrder.owner, orders);

    }

    /**
     * Calculate a Head-to-Head Move Order's <i>Defend Strength</i><br><br>
     *
     * Defend Strength is defined as the strength of a Move Order engaged in a Head-to-Head Battle (with another Mover),
     * which prevents the opposing Mover from succeeding.
     *
     * @param headToHeadMoveOrder Move Order whose Defend Strength to calculate
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of `headToHeadMoveOrder` and its opponents (inverted for opponents)
     * @param orders Collection of Orders to search
     * @return Defend Strength of `headToHeadMoveOrder`
     */
    private int calculateDefendStrength(Order headToHeadMoveOrder, boolean optimistic, Collection<Order> orders) {

        if (headToHeadMoveOrder.orderType != OrderType.MOVE)  // Does not check if the Move Order is indeed Head-to-Head
            throw new IllegalArgumentException(String.format("Non-Move Order supplied for `calculateDefendStrength(...)`: %s", headToHeadMoveOrder));

        return 1+tallySuccessfulSupports(headToHeadMoveOrder, !optimistic, orders);

    }

    /**
     * Calculate a Move Order's <i>Prevent Strength</i><br><br>
     *
     * Prevent Strength is defined as the strength (of a Move Order) preventing another Move Order (NOT engaged in Head-to-Head) from succeeding
     *
     * @param moveOrder Move Order whose Prevent Strength to calculate
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of `moveOrder` and its opponents (inverted for opponents)
     * @param orders Collection of Orders to search
     * @return Prevent Strength of `moveOrder`
     */
    private int calculatePreventStrength(Order moveOrder, boolean optimistic, Collection<Order> orders) {

        if (moveOrder.orderType != OrderType.MOVE)  // Does not check if the Move Order is indeed Non-Head-to-Head
            throw new IllegalArgumentException(String.format("Non-Move Order supplied for `calculatePreventStrength(...)`: %s", moveOrder));

        if (!pathSuccessful(moveOrder, optimistic, orders))
            return 0;

        Order headToHead = Orders.locateHeadToHead(moveOrder, orders);
        if (headToHead != null) {
            if (resolve(headToHead, !optimistic))
                return 0;
        }

        return 1+tallySuccessfulSupports(moveOrder, optimistic, orders);

    }

    /**
     * Calculate a Province's <i>Hold Strength</i><br><br>
     *
     * Hold Strength is defined as the strength (of a Province) preventing other [Move] Orders from moving to it<br>
     * If the area is empty, the value is 0.
     *
     * @param pos Province whose Hold Strength to calculate
     * @param optimistic Whether to resolve (& adjudicate) for the best-case or worst-case of Support-to-Hold Orders corresponding to Province `pos`
     * @param orders Collection of Orders to search
     * @return Hold Strength of Province `pos`
     */
    private int calculateHoldStrength(Province pos, boolean optimistic, Collection<Order> orders) {

        Order occupant = Orders.locateUnitAtPosition(pos, orders);
        if (occupant == null)
            return 0;

        if (occupant.orderType == OrderType.MOVE) {
            if (resolve(occupant, optimistic))
                return 0;
            else
                return 1;
        }

        return 1+tallySuccessfulSupports(occupant, optimistic, orders);

    }

}