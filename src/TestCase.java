import java.util.*;

public class TestCase {

    private final String name;
    private final List<Order> orders = new ArrayList<>();

    private List<boolean[]> expectedFields = null;
    private List<boolean[]> actualFields = null;
    private int score = 0;

    String eval = null;

    public TestCase(String name) {
        this.name = name;
    }

    public TestCase(String name, Order... orders) {
        this(name);
        List<Order> ordersList = Arrays.asList(orders);
        //Collections.shuffle(ordersList);
        this.orders.addAll(ordersList);
        actualFields = new ArrayList<>(this.orders.size());
        score = this.orders.size();
    }


    public void eval(boolean print) {

        StringBuilder output = new StringBuilder(this.name.toUpperCase()+":\n\n");

        if (expectedFields == null) {  // Debug output -- no expected fields

            for (Order order : orders)
                output.append(String.format("%s\n\t%s\n", order.toString(), order.metaToString()));

        } else {

            if (expectedFields.size() != orders.size())
                throw new IndexOutOfBoundsException("`expectedFields.size()` != `orders.size()`");

            this.judge();

            for (int i = 0; i < orders.size(); i++) {
                if (!Arrays.equals(expectedFields.get(i),actualFields.get(i)))
                    score--;
                output.append(String.format("%s\n\t%s\n\t%s:%s\n\t%s:%s\n",
                        orders.get(i).toString(), orders.get(i).metaToString(),
                        "EXPECTED", Arrays.toString(expectedFields.get(i)),
                        "ACTUAL", Arrays.toString(actualFields.get(i))));
            }

            output.append(String.format("\nSCORE: %d/%d", score, orders.size()));

            this.eval = output.toString();
            if (print)
                this.printEval();

        }

    }

    public void eval() {
        eval(false);
    }

    public void printEval() {
        System.out.println(eval+"\n\n");
    }

    private void judge() {

        Judge judge;
        if (!orders.isEmpty())
            judge = new Judge(new ArrayList<>(orders));
        else
            judge = new Judge();

        judge.judge();

        for (Order order : judge.getOrders())
            actualFields.add(new boolean[]{order.verdict});  // Can expand with more fields later

    }


    public void setExpectedFields(boolean[]... fields) {

        if (fields.length != orders.size())
            throw new IndexOutOfBoundsException("`len(fields)` != `orders.size()`");

        expectedFields = new ArrayList<>();
        expectedFields.addAll(Arrays.asList(fields));

    }

    public void setExpectedFields(boolean... fields) {

        if (fields.length != orders.size())
            throw new IndexOutOfBoundsException("`len(fields)` != `orders.size()`");

        expectedFields = new ArrayList<>();
        for (boolean field : fields)
            expectedFields.add(new boolean[]{field});

    }


    public String getName() {
        return name;
    }

    public List<Order> getOrders() {
        return orders;
    }

}