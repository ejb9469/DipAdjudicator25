import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum Province {

    // TODO: Coast support

    Boh("Bohemia", false, false),
    Bud("Budapest", false, true),
    Gal("Galicia", false, false),
    Tri("Trieste", false, true),
    Tyr("Tyrolia", true, false),
    Vie("Vienna", false, true),
    Cly("Clyde", true, false),
    Edi("Edinburgh", true, true),
    Lvp("Liverpool", true, true),
    Lon("London", true, true),
    Wal("Wales", true, false),
    Yor("Yorkshire", true, false),
    Bre("Brest", true, true),
    Bur("Burgundy", false, false),
    Gas("Gascony", true, false),
    Mar("Marseilles", true, true),
    Par("Paris", false, true),
    Pic("Picardy", true, false),
    Ber("Berlin", true, true),
    Kie("Kiel", true, true),
    Mun("Munich", false, true),
    Pru("Prussia", true, false),
    Ruh("Ruhr", false, false),
    Sil("Silesia", false, false),
    Apu("Apulia", true, false),
    Nap("Naples", true, true),
    Pie("Piedmont", true, false),
    Rom("Rome", true, true),
    Tus("Tuscany", true, false),
    Ven("Venice", true, true),
    Lvn("Livonia", true, false),
    Mos("Moscow", false, true),
    Sev("Sevastopol", true, true),
    Stp("St. Petersburg", true, true, false),
    Ukr("Ukraine", false, false),
    War("Warsaw", false, true),
    Ank("Ankara", true, true),
    Arm("Armenia", true, false),
    Con("Constantinople", true, true),
    Smy("Smyrna", true, true),
    Syr("Syria", true, false),
    Alb("Albania", true, false),
    Bel("Belgium", true, true),
    Bul("Bulgaria", true, true, false),
    Fin("Finland", true, false),
    Gre("Greece", true, true),
    Hol("Holland", true, true),
    Nwy("Norway", true, true),
    Naf("North Africa", true, false),
    Por("Portugal", true, true),
    Rum("Rumania", true, true),
    Ser("Serbia", false, true),
    Spa("Spain", true, true, false),
    Swe("Sweden", true, true),
    Tun("Tunis", true, true),
    Den("Denmark", true, true),
    ADR("Adriatic Sea", true),
    AEG("Aegean Sea", true),
    BAL("Baltic Sea", true),
    BAR("Barents Sea", true),
    BLA("Black Sea", true),
    EAS("Eastern Mediterranean", true),
    ENG("English Channel", true),
    BOT("Gulf of Bothnia", true),
    LYO("Gulf of Lyon", true),
    HEL("Helgoland Bight", true),
    ION("Ionian Sea", true),
    IRI("Irish Sea", true),
    MAO("Mid-Atlantic Ocean", true),
    NAO("North Atlantic Ocean", true),
    NTH("North Sea", true),
    NWG("Norwegian Sea", true),
    SKA("Skagerrak", true),
    TYS("Tyrrhenian Sea", true),
    WES("Western Mediterranean", true),
    StpNC("St. Petersburg (north coast)", true, true, true),
    StpSC("St. Petersburg (south coast)", true, true, true),
    SpaNC("Spain (north coast)", true, true, true),
    SpaSC("Spain (south coast)", true, true, true),
    BulEC("Bulgaria (east coast)", true, true, true),
    BulSC("Bulgaria (south coast)", true, true, true),
    Swi("Switzerland", false, false);

    private final String name;
    private final short waterAccess;
    private final boolean supplyCenter;
    private final boolean splitCoast;

    private static Map<Province, Province[]> adjacencyMap;

    private static final Map<String, Province> validNames = new HashMap<>();  //{"Bohemia", "Budapest"};  // TODO


    public static Map<String, String> generateFullNamesToAbbreviationsMap() {

        Map<String, String> map = new HashMap<>();

        for (Province province : Province.values()) {
            map.put(province.getName(), province.name());
        }
        return map;

    }

    private Province(String name, boolean coastal, boolean supplyCenter, boolean splitCoast) {
        populateAdjacencyMap();
        this.name = name;
        this.supplyCenter = supplyCenter;
        if (!coastal)
            this.waterAccess = 0;
        else
            this.waterAccess = 1;
        this.splitCoast = splitCoast;
    }

    private Province(String name, boolean coastal, boolean supplyCenter) {  // Assumed land province
        populateAdjacencyMap();
        this.name = name;
        this.supplyCenter = supplyCenter;
        if (!coastal)
            this.waterAccess = 0;
        else
            this.waterAccess = 1;
        splitCoast = false;
    }

    private Province(String name, boolean water) {  // Assumes no supply centers in water, DETERMINISTIC
        populateAdjacencyMap();
        this.name = name;
        this.waterAccess = 2;
        this.supplyCenter = false;
        splitCoast = false;
    }

    boolean isAdjacentTo(Province province) {
        if (province == Province.Swi || this == Province.Swi) return false;
        return (Arrays.asList(adjacencyMap.get(this)).contains(province));
    }

    boolean isCoastal() {
        return (waterAccess == 1);
    }

    boolean isWater() {
        return (waterAccess == 2);
    }

    boolean isSupplyCenter() {
        return supplyCenter;
    }

    public boolean hasSplitCoast() {
        return splitCoast;
    }

    String getName() {
        return name;
    }

    boolean correctName(String n) {
        return name.equals(n);
    }

    static boolean isValidName(String n) {
        return validNames.containsKey(n);
    }

    static Province convertToEnum(String n) {
        return validNames.get(n);
    }

    // No setter tbd

    private static void populateAdjacencyMap() {

        adjacencyMap = new HashMap<>();

        adjacencyMap.put(Province.Boh, new Province[]{Province.Mun, Province.Sil, Province.Gal, Province.Tyr, Province.Vie});
        adjacencyMap.put(Province.Bud, new Province[]{Province.Vie, Province.Gal, Province.Tri, Province.Ser, Province.Rum});
        adjacencyMap.put(Province.Gal, new Province[]{Province.Sil, Province.Boh, Province.Vie, Province.Bud, Province.Rum, Province.Ukr, Province.War});
        adjacencyMap.put(Province.Tri, new Province[]{Province.Tyr, Province.Ven, Province.ADR, Province.Alb, Province.Ser, Province.Bud, Province.Vie});
        adjacencyMap.put(Province.Tyr, new Province[]{Province.Mun, Province.Boh, Province.Vie, Province.Tri, Province.Ven, Province.Pie});
        adjacencyMap.put(Province.Vie, new Province[]{Province.Tyr, Province.Boh, Province.Gal, Province.Bud, Province.Tri});
        adjacencyMap.put(Province.Cly, new Province[]{Province.NAO, Province.NWG, Province.Edi, Province.Lvp});
        adjacencyMap.put(Province.Edi, new Province[]{Province.Cly, Province.NWG, Province.NTH, Province.Yor, Province.Lvp});
        adjacencyMap.put(Province.Lvp, new Province[]{Province.NAO, Province.Cly, Province.Edi, Province.Yor, Province.Wal, Province.IRI});
        adjacencyMap.put(Province.Lon, new Province[]{Province.Wal, Province.Yor, Province.NTH, Province.ENG});
        adjacencyMap.put(Province.Wal, new Province[]{Province.Lvp, Province.Yor, Province.Lon, Province.ENG, Province.IRI});
        adjacencyMap.put(Province.Yor, new Province[]{Province.Edi, Province.NTH, Province.Lon, Province.Wal, Province.Lvp});
        adjacencyMap.put(Province.Bre, new Province[]{Province.ENG, Province.Pic, Province.Par, Province.Gas, Province.MAO});
        adjacencyMap.put(Province.Bur, new Province[]{Province.Bel, Province.Ruh, Province.Mun, Province.Mar, Province.Gas, Province.Par, Province.Pic});
        adjacencyMap.put(Province.Gas, new Province[]{Province.Bre, Province.Par, Province.Bur, Province.Mar, Province.Spa, Province.SpaNC, Province.MAO});
        adjacencyMap.put(Province.Mar, new Province[]{Province.Bur, Province.Pie, Province.LYO, Province.Spa, Province.SpaSC, Province.Gas});
        adjacencyMap.put(Province.Par, new Province[]{Province.Bre, Province.Pic, Province.Bur, Province.Gas});
        adjacencyMap.put(Province.Pic, new Province[]{Province.ENG, Province.Bel, Province.Bur, Province.Par, Province.Bre});
        adjacencyMap.put(Province.Ber, new Province[]{Province.BAL, Province.Pru, Province.Sil, Province.Mun, Province.Kie});
        adjacencyMap.put(Province.Kie, new Province[]{Province.Den, Province.BAL, Province.Ber, Province.Mun, Province.Ruh, Province.Hol, Province.HEL});
        adjacencyMap.put(Province.Mun, new Province[]{Province.Kie, Province.Ber, Province.Sil, Province.Boh, Province.Tyr, Province.Bur, Province.Ruh});
        adjacencyMap.put(Province.Pru, new Province[]{Province.BAL, Province.Lvn, Province.War, Province.Sil, Province.Ber});
        adjacencyMap.put(Province.Ruh, new Province[]{Province.Hol, Province.Kie, Province.Mun, Province.Bur, Province.Bel});
        adjacencyMap.put(Province.Sil, new Province[]{Province.Ber, Province.Pru, Province.War, Province.Gal, Province.Boh, Province.Mun});
        adjacencyMap.put(Province.Apu, new Province[]{Province.ADR, Province.ION, Province.Nap, Province.Rom, Province.Ven});
        adjacencyMap.put(Province.Nap, new Province[]{Province.Apu, Province.ION, Province.TYS, Province.Rom});
        adjacencyMap.put(Province.Pie, new Province[]{Province.Tyr, Province.Ven, Province.Tus, Province.LYO, Province.Mar});
        adjacencyMap.put(Province.Rom, new Province[]{Province.Ven, Province.Apu, Province.Nap, Province.TYS, Province.Tus});
        adjacencyMap.put(Province.Tus, new Province[]{Province.Ven, Province.Rom, Province.TYS, Province.LYO, Province.Pie});
        adjacencyMap.put(Province.Ven, new Province[]{Province.Tyr, Province.Tri, Province.ADR, Province.Apu, Province.Rom, Province.Tus, Province.Pie});
        adjacencyMap.put(Province.Lvn, new Province[]{Province.BOT, Province.Stp, Province.StpSC, Province.Mos, Province.War, Province.Pru, Province.BAL});
        adjacencyMap.put(Province.Mos, new Province[]{Province.Stp, Province.Sev, Province.Ukr, Province.War, Province.Lvn});
        adjacencyMap.put(Province.Sev, new Province[]{Province.Mos, Province.Arm, Province.BLA, Province.Rum, Province.Ukr});
        adjacencyMap.put(Province.Stp, new Province[]{Province.BAR, Province.Mos, Province.Lvn, Province.BOT, Province.Fin, Province.Nwy});
        adjacencyMap.put(Province.Ukr, new Province[]{Province.Mos, Province.Sev, Province.Rum, Province.Gal, Province.War});
        adjacencyMap.put(Province.War, new Province[]{Province.Lvn, Province.Mos, Province.Ukr, Province.Gal, Province.Sil, Province.Pru});
        adjacencyMap.put(Province.Ank, new Province[]{Province.BLA, Province.Arm, Province.Smy, Province.Con});
        adjacencyMap.put(Province.Arm, new Province[]{Province.Sev, Province.Syr, Province.Smy, Province.Ank, Province.BLA});
        adjacencyMap.put(Province.Con, new Province[]{Province.BLA, Province.Ank, Province.Smy, Province.AEG, Province.Bul, Province.BulEC, Province.BulSC});
        adjacencyMap.put(Province.Smy, new Province[]{Province.Ank, Province.Arm, Province.Syr, Province.EAS, Province.AEG, Province.Con});
        adjacencyMap.put(Province.Syr, new Province[]{Province.Arm, Province.EAS, Province.Smy});
        adjacencyMap.put(Province.Alb, new Province[]{Province.Tri, Province.Ser, Province.Gre, Province.ION, Province.ADR});
        adjacencyMap.put(Province.Bel, new Province[]{Province.NTH, Province.Hol, Province.Ruh, Province.Bur, Province.Pic, Province.ENG});
        adjacencyMap.put(Province.Bul, new Province[]{Province.Rum, Province.BLA, Province.Con, Province.AEG, Province.Gre, Province.Ser});
        adjacencyMap.put(Province.Fin, new Province[]{Province.Nwy, Province.Stp, Province.StpSC, Province.BOT, Province.Swe});
        adjacencyMap.put(Province.Gre, new Province[]{Province.Ser, Province.Bul, Province.BulSC, Province.AEG, Province.ION, Province.Alb});
        adjacencyMap.put(Province.Hol, new Province[]{Province.NTH, Province.HEL, Province.Kie, Province.Ruh, Province.Bel});
        adjacencyMap.put(Province.Nwy, new Province[]{Province.NWG, Province.BAR, Province.Stp, Province.StpNC, Province.Fin, Province.Swe, Province.SKA, Province.NTH});
        adjacencyMap.put(Province.Naf, new Province[]{Province.WES, Province.Tun, Province.MAO});
        adjacencyMap.put(Province.Por, new Province[]{Province.Spa, Province.SpaNC, Province.SpaSC, Province.MAO});
        adjacencyMap.put(Province.Rum, new Province[]{Province.Ukr, Province.Sev, Province.BLA, Province.Bul, Province.BulEC, Province.Ser, Province.Bud, Province.Gal});
        adjacencyMap.put(Province.Ser, new Province[]{Province.Bud, Province.Rum, Province.Bul, Province.Gre, Province.Alb, Province.Tri});
        adjacencyMap.put(Province.Spa, new Province[]{Province.Gas, Province.Mar, Province.LYO, Province.WES, Province.MAO, Province.Por});
        adjacencyMap.put(Province.Swe, new Province[]{Province.Nwy, Province.Fin, Province.BOT, Province.BAL, Province.Den, Province.SKA});
        adjacencyMap.put(Province.Tun, new Province[]{Province.TYS, Province.ION, Province.Naf, Province.WES});
        adjacencyMap.put(Province.Den, new Province[]{Province.SKA, Province.Swe, Province.BAL, Province.Kie, Province.HEL, Province.NTH});
        adjacencyMap.put(Province.ADR, new Province[]{Province.Tri, Province.Alb, Province.ION, Province.Apu, Province.Ven});
        adjacencyMap.put(Province.AEG, new Province[]{Province.Bul, Province.BulSC, Province.Con, Province.Smy, Province.EAS, Province.ION, Province.Gre});
        adjacencyMap.put(Province.BAL, new Province[]{Province.Swe, Province.BOT, Province.Lvn, Province.Pru, Province.Ber, Province.Kie, Province.Den});
        adjacencyMap.put(Province.BAR, new Province[]{Province.NWG, Province.Nwy, Province.Stp, Province.StpNC});
        adjacencyMap.put(Province.BLA, new Province[]{Province.Sev, Province.Arm, Province.Ank, Province.Con, Province.Bul, Province.BulEC, Province.Rum});
        adjacencyMap.put(Province.EAS, new Province[]{Province.Smy, Province.Syr, Province.ION, Province.AEG});
        adjacencyMap.put(Province.ENG, new Province[]{Province.Lon, Province.NTH, Province.Bel, Province.Pic, Province.Bre, Province.MAO, Province.IRI, Province.Wal});
        adjacencyMap.put(Province.BOT, new Province[]{Province.Fin, Province.Stp, Province.StpSC, Province.Lvn, Province.BAL, Province.Swe});
        adjacencyMap.put(Province.LYO, new Province[]{Province.Mar, Province.Pie, Province.Tus, Province.TYS, Province.WES, Province.Spa, Province.SpaSC});
        adjacencyMap.put(Province.HEL, new Province[]{Province.NTH, Province.Den, Province.Kie, Province.Hol});
        adjacencyMap.put(Province.ION, new Province[]{Province.ADR, Province.Alb, Province.Gre, Province.AEG, Province.EAS, Province.Tun, Province.TYS, Province.Nap, Province.Apu});
        adjacencyMap.put(Province.IRI, new Province[]{Province.NAO, Province.Lvp, Province.Wal, Province.ENG, Province.MAO});
        adjacencyMap.put(Province.MAO, new Province[]{Province.NAO, Province.IRI, Province.ENG, Province.Bre, Province.Gas, Province.Spa, Province.SpaNC, Province.SpaSC, Province.Por, Province.WES, Province.Naf});
        adjacencyMap.put(Province.NAO, new Province[]{Province.NWG, Province.Cly, Province.Lvp, Province.IRI, Province.MAO});
        adjacencyMap.put(Province.NTH, new Province[]{Province.NWG, Province.Nwy, Province.SKA, Province.Den, Province.HEL, Province.Hol, Province.Bel, Province.ENG, Province.Lon, Province.Yor, Province.Edi});
        adjacencyMap.put(Province.NWG, new Province[]{Province.BAR, Province.Nwy, Province.NTH, Province.Edi, Province.Cly, Province.NAO});
        adjacencyMap.put(Province.SKA, new Province[]{Province.Nwy, Province.Swe, Province.Den, Province.NTH});
        adjacencyMap.put(Province.TYS, new Province[]{Province.Tus, Province.Rom, Province.Nap, Province.ION, Province.Tun, Province.WES, Province.LYO});
        adjacencyMap.put(Province.WES, new Province[]{Province.LYO, Province.TYS, Province.Tun, Province.Naf, Province.Spa, Province.SpaSC});
        adjacencyMap.put(Province.StpNC, new Province[]{Province.BAR, Province.Nwy});
        adjacencyMap.put(Province.StpSC, new Province[]{Province.Fin, Province.Lvn, Province.BOT});
        adjacencyMap.put(Province.SpaNC, new Province[]{Province.MAO, Province.Gas, Province.Por});
        adjacencyMap.put(Province.SpaSC, new Province[]{Province.MAO, Province.Mar, Province.LYO, Province.WES, Province.Por});
        adjacencyMap.put(Province.BulEC, new Province[]{Province.Rum, Province.BLA, Province.Con});
        adjacencyMap.put(Province.BulSC, new Province[]{Province.Con, Province.AEG, Province.Gre});
        adjacencyMap.put(Province.Swi, new Province[]{});  // teehee

    }

}