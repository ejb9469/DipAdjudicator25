public interface Snapshot<T> {

    public   T            getSnapshot();
    public   void         takeSnapshot();
    public   void         restoreFromSnapshot();

}
