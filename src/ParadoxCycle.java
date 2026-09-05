import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of an active recursive dependency cycle detected while
 * adjudicating one collection of Diplomacy orders.<br>
 *
 * This is deliberately separate from Judge.cycle. Judge.cycle remains part of
 * the legacy resolution-control protocol; ParadoxCycle records the actual
 * active call-chain slice at the instant recursion revisits an active order.
 */
public final class ParadoxCycle {


    private final List<Order> members;


    public ParadoxCycle(List<Order> members) {
        this.members = Collections.unmodifiableList(
                new ArrayList<>(members)
        );
    }


    /**
     * Returns the exact Order object references that participated in this
     * recursive cycle. They are not cloned because this record is intended for
     * use only during one Judge.judge() invocation.
     */
    public List<Order> getMembers() {
        return this.members;
    }

    /**
     * Returns true when at least one member is a convoy order.
     *
     * A convoy transformed by the existing Szykman mechanism is also treated as
     * a convoy if it retains an original-order snapshot.
     */
    public boolean containsConvoy() {
        for (Order order : members) {
            if (order.orderType == OrderType.CONVOY)
                return true;

            Order originalOrder = order.getSnapshot();

            if (originalOrder != null
                    && originalOrder.orderType == OrderType.CONVOY) {
                return true;
            }
        }

        return false;
    }

    /**
     * Produces a stable display key for de-duplicating and logging cycles.
     *
     * This is based on the submitted-order identity, not resolved/verdict
     * metadata, since metadata can change during adjudication.
     */
    public String key() {
        List<String> entries = new ArrayList<>();

        for (Order order : members) {
            Order originalOrder = order.getSnapshot();

            if (originalOrder == null)
                originalOrder = order;

            entries.add(
                    originalOrder.owner
                            + "|" + originalOrder.unitType
                            + "|" + originalOrder.orderType
                            + "|" + originalOrder.pos0
                            + "|" + originalOrder.pos1
                            + "|" + originalOrder.pos2
            );
        }

        Collections.sort(entries);
        return String.join("\n", entries);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        output.append("ParadoxCycle[")
                .append("members=")
                .append(members.size())
                .append(", containsConvoy=")
                .append(containsConvoy())
                .append("]");

        for (int i = 0; i < members.size(); i++) {
            output.append(System.lineSeparator())
                    .append("  [")
                    .append(i)
                    .append("] ")
                    .append(members.get(i));
        }

        return output.toString();
    }

}