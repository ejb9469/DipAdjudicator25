import java.util.*;

public class TestCase {

    public static final String TESTCASE_PREFIX = "TEST CASE - ";

    private final List<Order>   orders;
    private String              name;

    private List<boolean[]>     expectedFields = null;
    private List<boolean[]>     actualFields = null;
    private int                 score = 0;
    private String              eval = null;


    public TestCase(String name, Order... orders) {
        this.name = name;
        List<Order> ordersList = Arrays.asList(orders);
        //Collections.shuffle(ordersList);  // Cannot shuffle here, must be done layer(s) up
        this.orders = new ArrayList<>(ordersList);
        this.actualFields = new ArrayList<>(this.orders.size());
        this.score = this.orders.size();
    }

    public TestCase(String name, List<Order> orders) {
        this.name = name;
        this.orders = new ArrayList<>(orders);
        this.actualFields = new ArrayList<>(this.orders.size());
        this.score = this.orders.size();
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


    public void eval(boolean print) {

        String name = TESTCASE_PREFIX+this.name;
        StringBuilder output = new StringBuilder(name+":\n\n");

        if (expectedFields == null) {  // Debug output -- no expected fields

            System.out.println("NO EXPECTED FIELDS for...\t\t[" + name + "]");
            for (Order order : orders)
                output.append(String.format("%s\n\t%s\n", order.toString(), order.metaToString()));

        } else {

            if (expectedFields.size() != orders.size())
                throw new IndexOutOfBoundsException("`expectedFields.size()` != `orders.size()`");

            this.judge();

            for (int i = 0; i < orders.size(); i++) {
                if (!Arrays.equals(expectedFields.get(i), actualFields.get(i)))
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

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public String getEval() {
        return eval;
    }

    public int getScore() {
        return score;
    }


    public void printEval() {
        System.out.println(eval+"\n\n");
    }

    public void printNameAndScore() {
        System.out.printf("[%02d/%02d]\t%s%s\n", this.score, this.orders.size(), TESTCASE_PREFIX, this.getName());
    }

}