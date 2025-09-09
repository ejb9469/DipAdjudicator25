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
        //testCaseA.eval(true);

        TestCase testCaseB = new TestCase("Test Case 2 -- Example",
                new Order(Nation.AUSTRIA, UnitType.FLEET, Province.Tri, OrderType.HOLD),
                new Order(Nation.ITALY, UnitType.ARMY, Province.Ven, OrderType.MOVE, Province.Tri),
                new Order(Nation.ITALY, UnitType.ARMY, Province.Tyr, OrderType.SUPPORT, Province.Ven, Province.Tri),
                new Order(Nation.GERMANY, UnitType.ARMY, Province.Mun, OrderType.MOVE, Province.Tyr),
                new Order(Nation.RUSSIA, UnitType.ARMY, Province.Sil, OrderType.MOVE, Province.Mun),
                new Order(Nation.RUSSIA, UnitType.ARMY, Province.Ber, OrderType.SUPPORT, Province.Sil, Province.Mun));

        testCaseB.setExpectedFields(true, false, false, false, true, true);
        testCaseB.eval(true);

    }

}