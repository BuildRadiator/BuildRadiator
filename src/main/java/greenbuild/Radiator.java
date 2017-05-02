package greenbuild;

import java.util.ArrayList;

public class Radiator {

  public ArrayList<Build> builds = new ArrayList<>();

  private transient final String[] stepNames;

  public Radiator(String... stepNames) {
    this.stepNames = stepNames;
  }

  public Radiator startStep(int build, String step) {
    boolean found = false;
    for (Build b : this.builds) {
      if (b.num == build) {
        b.start(step);
        found = true;
        break;
      }
    }
    if (!found) {
      synchronized (builds) {
        builds.add(0, new Build(build, stepNames));
        startStep(build, step);
        if (builds.size() == 11) {
          builds.remove(10);
        }
      }
    }
    return this;
  }

  public void stepPassed(int build, String step) {
    for (Build b : this.builds) {
      if (b.num == build) {
        b.stepPassed(step);
        break;
      }
    }

  }

  public void stepFailed(int build, String step) {
    for (Build b : this.builds) {
      if (b.num == build) {
        b.stepFailed(step);
        break;
      }
    }

  }

}
