package greenbuild;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

@Singleton
public class RadiatorStore {

  protected Map<String, Radiator> results = new ConcurrentHashMap<>();

  public RadiatorStore() {
  }

  public Radiator get(final String projCode) {
    Radiator radiator = this.results.get(projCode);
    return radiator;
  }

  public Radiator createRadiator(String projCode, String... steps) {
    Radiator radiator = new Radiator(steps);
    this.results.put(projCode, radiator);
    return radiator;
  }

}
