import java.util.ArrayList;
import java.util.List;

public class TestCaseReferee extends TestCase {


    public TestCaseReferee(String name, Order... orders) {
        super(name, orders);
    }

    public TestCaseReferee(String name, List<Order> orders) {
        super(name, orders);
    }

    public TestCaseReferee(TestCase testCase) {
        super(testCase);
    }

    @Override
    protected void judge() {

        Judge judge;
        if (!orders.isEmpty())
            judge = new Referee(new ArrayList<>(orders));
        else
            judge = new Referee();

        judge.judge();
        judge.orders = Orders.conformOrder(judge.orders, this.orders);

        for (Order order : judge.getOrders())
            actualFields.add(new boolean[]{order.verdict});  // Could expand with more fields later

    }


}
