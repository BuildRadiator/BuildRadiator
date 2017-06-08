package com.paulhammant.buildradiator;

import com.paulhammant.buildradiator.model.NotAnIPAddress;
import com.paulhammant.buildradiator.model.Radiator;
import com.paulhammant.buildradiator.model.RadiatorDoesntExist;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertNull;

public class RadiatorStoreUnitTest {

    private String radCode;
    private Radiator rad;
    private int called;
    private RadiatorStore radStore;

    @Before
    public void setup() {
        radCode = null;
        rad = null;
        called = 0;
        radStore = null;
    }

    @After
    public void tearDown() {
        if (radStore != null) {
            radStore.stopSaver();
        }
    }

    @Test
    public void radiatorCanBeCreatedWithGoodIpAddresses() {
        Radiator rad = new RadiatorStore().createRadiator(new TestRandomGenerator("QWERTY", "sseeccrreett"), "A")
                .withIpAccessRestrictedToThese("123.123.123.122", "2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        assertThat(rad.code, equalTo("QWERTY"));
        assertThat(rad.codeAndSecretOnly().code, equalTo("QWERTY"));
        assertThat(rad.codeAndSecretOnly().secret, equalTo("sseeccrreett"));
    }

    @Test
    public void radiatorCannotBeCreatedWithBadIpAddresses() {
        try {
            new RadiatorStore().createRadiator(new TestRandomGenerator("QWERTY", "sseeccrreett"), "A")
                    .withIpAccessRestrictedToThese("555.123.123.122");
            fail();
        } catch (NotAnIPAddress e) {
         }
        try {
            new RadiatorStore().createRadiator(new TestRandomGenerator("QWERTY", "sseeccrreett"), "A")
                    .withIpAccessRestrictedToThese("2001:0dx8:85a3:0000:0000:8a2e:0370:7334");
            fail();
        } catch (NotAnIPAddress e) {
        }
    }

    @Test
    public void radiatorCanBeRetrieved() {
        RadiatorStore rs = new RadiatorStore();
        Radiator rad = null;
        try {
            rad = rs.get("AAA", "127.0.0.1");
            fail();
        } catch (RadiatorDoesntExist e) {
            // expected
        }
        rs.createRadiator(new TestRandomGenerator("AAA", "sseeccrreett"), "A");
        rad = rs.get("AAA", "127.0.0.1");
        assertThat(rad.code, equalTo("AAA"));
    }

    @Test
    public void saverThreadShouldDoItsBusinessForUnsavedRadiatorJustOnce() throws InterruptedException {
        radStore = getTestAwareRadiatorStore();
        radStore.createRadiator(new TestRandomGenerator("QWERTY", "sseeccrreett"), "A");
        assertQwertyRadiatorHasBeenCalledOnce();
    }

    @Test
    public void saverThreadShouldDoItsBusinessForUnsavedRadiatorTwiceIfNeeded() throws InterruptedException {
        saverThreadShouldDoItsBusinessForUnsavedRadiatorJustOnce();
        Radiator rad = radStore.get("QWERTY", "127.0.0.1");
        rad.lastUpdated = System.currentTimeMillis();
        Thread.sleep(10);
        assertThat(called, equalTo(2));
    }

    @Test
    public void unknownRadiatorShouldBeSoughtFromDataServiceJustOnce() throws InterruptedException {
        radStore = getTestAwareRadiatorStore();
        Radiator r = radStore.createRadiator(new TestRandomGenerator("QWERTY", "sseeccrreett"), "A");
        assertQwertyRadiatorHasBeenCalledOnce();
    }

    private void assertQwertyRadiatorHasBeenCalledOnce() throws InterruptedException {
        Thread.sleep(10);
        assertThat(radCode, equalTo("QWERTY"));
        assertThat(rad, sameInstance(rad));
        assertThat(called, equalTo(1));
    }

    private RadiatorStore getTestAwareRadiatorStore() {
        return new RadiatorStore() {
            @Override
            protected void saveInDataService(String radCode, Radiator rad) {
                RadiatorStoreUnitTest.this.radCode = radCode;
                RadiatorStoreUnitTest.this.rad = rad;
                called++;
            }

            @Override
            protected int getMillisToDelay() {
                return 1;
            }
        };
    }

}
