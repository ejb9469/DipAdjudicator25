import java.util.ArrayList;
import java.util.List;

public class CourtManager {

    private final List<TestCase> testCases;


    public CourtManager() {
        this.testCases = new ArrayList<>();
    }


    public List<TestCase> getTestCases() {
        return testCases;
    }


    public static void main(String[] args) {

        TestCase testCaseA = new TestCase("Test Case 1 -- Example",
                new Order(Nation.ENGLAND, UnitType.FLEET, Province.ENG, OrderType.MOVE, Province.Bre),
                new Order(Nation.FRANCE, UnitType.FLEET, Province.Bre, OrderType.MOVE, Province.MAO),
                new Order(Nation.ITALY, UnitType.FLEET, Province.MAO, OrderType.MOVE, Province.ENG),
                new Order(Nation.AUSTRIA, UnitType.FLEET, Province.Wal, OrderType.MOVE, Province.ENG),
                new Order(Nation.GERMANY, UnitType.FLEET, Province.IRI, OrderType.SUPPORT, Province.Wal, Province.ENG));

        testCaseA.setExpectedFields(false, false, false, true, true);

        testCaseA.eval();

    }

}