import java.util.*;

public class TestCase {

    private final String name;
    private final List<Order> orders = new ArrayList<>();

    private List<boolean[]> expectedFields = null;
    private List<boolean[]> actualFields = null;
    private int score = 0;

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


    public void eval() {

        if (expectedFields == null) {  // Debug output

            System.out.println(this.name.toUpperCase()+":");
            for (Order order : orders)
                System.out.printf("%s\n\t%s\n", order.toString(), order.metaToString());

        } else {

            if (expectedFields.size() != orders.size())
                throw new IndexOutOfBoundsException("`expectedFields.size()` != `orders.size()`");

            this.judge();

            System.out.println(this.name.toUpperCase()+":");

            for (int i = 0; i < orders.size(); i++) {
                if (!Arrays.equals(expectedFields.get(i),actualFields.get(i)))
                    score--;
                System.out.printf("%s\n\t%s\n\t%s:%s\n\t%s:%s\n",
                        orders.get(i).toString(), orders.get(i).metaToString(),
                        "EXPECTED", Arrays.toString(expectedFields.get(i)),
                        "ACTUAL", Arrays.toString(actualFields.get(i)));
            }

            System.out.printf("SCORE: %d/%d\n", score, orders.size());

        }

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