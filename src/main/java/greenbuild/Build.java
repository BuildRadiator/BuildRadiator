package greenbuild;

import java.util.ArrayList;

public class Build {
    public int num;
    public ArrayList<Step> steps = new ArrayList<>();
    public String status = "";

    public Build(int num, String[] steps) {
        this.num = num;
        for (String step : steps) {
            this.steps.add(new Step(step));
        }
    }

    public void start(String step) {
        for (Step s : steps) {
            if (s.name.equals(step)) {
                s.start();
                break;
            }
        }
        status = "running";
    }

    public void stepPassed(String step) {
        boolean passed = true;
        for (Step s : steps) {
            if (s.name.equals(step)) {
                s.pass();
            }
            if (!s.status.equals("passed")) {
                passed = false;
            }
        }
        status = passed ? "passed" : status;
    }

    public void stepFailed(String step) {
        boolean failureNoted = false;
        for (Step s : steps) {
            if (s.name.equals(step)) {
                s.fail();
                failureNoted = true;
                continue;
            }
            if (failureNoted) {
               s.skipped();
            }

        }
        status = "failed";

    }
}
