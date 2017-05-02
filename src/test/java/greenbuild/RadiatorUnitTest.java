package greenbuild;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class RadiatorUnitTest {

    @Test
    public void buildCanHaveThreeStepsAndFailOnSecond() throws InterruptedException, JsonProcessingException {
        Radiator res = new RadiatorStore().createRadiator("abc123", "compile", "unit tests", "func tests");

        res.startStep(123, "compile");
        Thread.sleep(50); // test on timing too
        res.stepPassed(123, "compile");

        res.startStep(123, "unit tests");
        res.stepFailed(123, "unit tests");

        assertThat(new ObjectMapper().writeValueAsString(res)
                        .replace(":55,", ":50,")
                        .replace(":54,", ":50,")
                        .replace(":53,", ":50,")
                        .replace(":52,", ":50,")
                        .replace(":51,", ":50,")
                        .replace(":5,", ":0,")
                        .replace(":4,", ":0,")
                        .replace(":3,", ":0,")
                        .replace(":2,", ":0,")
                        .replace(":1,", ":0,")
                        .replace("\"",""),
                equalTo(("{builds:[{num:123,steps:[{name:compile,dur:50,status:passed}," +
                        "{name:unit tests,dur:0,status:failed}," +
                        "{name:func tests,dur:0,status:skipped}],status:failed}]}")
                        ));

    }

    @Test
    public void radiatorCanOnlyHoldTenBuildsAndInReverseOrder() throws InterruptedException, JsonProcessingException {
        Radiator res = new RadiatorStore().createRadiator("X", "A");

        for (int i = 1; i <= 11; i++) {
            res.startStep(i, "A");
        }

        assertThat(new ObjectMapper().writeValueAsString(res)
                        .replace("\"","")
                        .replace(",steps:[{name:A,dur:0,status:running}],status:running",""),
                equalTo(("{builds:[{num:11},{num:10},{num:9},{num:8},{num:7},{num:6},{num:5},{num:4},{num:3},{num:2}]}")));

    }

}
