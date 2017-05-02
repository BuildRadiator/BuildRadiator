package greenbuild;

public class Step {

    public String name;
    public long dur;
    public String status = "";
    private transient long started;

    public Step(String name) {
        this.name = name;
    }

    public void start() {
        started = System.currentTimeMillis();
        status = "running";
    }

    public void pass() {
        complete();
        status = "passed";
    }

    private void complete() {
        dur = System.currentTimeMillis() - started;
    }

    public void fail() {
        complete();
        status = "failed";
    }

    public void skipped() {
        status = "skipped";
    }
}
