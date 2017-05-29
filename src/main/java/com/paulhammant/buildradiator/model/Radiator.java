package com.paulhammant.buildradiator.model;

import com.google.common.net.InetAddresses;

import java.util.ArrayList;

import static com.paulhammant.buildradiator.BuildRadiatorApp.NO_UPDATES;

public class Radiator {

    public final ArrayList<Build> builds = new ArrayList<>();
    public long lastUpdated;
    public String[] stepNames;
    public String[] ips = new String[0];
    public String code;
    public String secret;

    public Radiator() {
    }

    public Radiator(String code, String secret, String... stepNames) {
        this.code = code;
        this.secret = secret;
        this.stepNames = stepNames;
    }

    public Radiator withIpAccessRestrictedToThese(String... ips) {
        this.ips = ips;
        for (String ip : ips) {
            if (!InetAddresses.isInetAddress(ip)) {
                throw new NotAnIPAddress();
            }
        }
        return this;
    }

    public void startStep(String buildNum, String step) {
        lastUpdated = System.currentTimeMillis();
        for (Build build : this.builds) {
            if (build.ref.equals(buildNum)) {
                build.start(step);
                return;
            }
        }
        // build not found
        synchronized (builds) {
            builds.add(0, new Build(buildNum, stepNames));
            if (builds.size() > 10) {
                builds.remove(10);
            }
            startStep(buildNum, step);
        }
    }

    public void stepPassed(String buildRef, String step) {
        lastUpdated = System.currentTimeMillis();
        for (Build build : this.builds) {
            if (build.ref.equals(buildRef)) {
                build.pass(step);
                return;
            }
        }
        throw new UnknownBuild();
    }

    public void stepFailed(String buildRef, String step) {
        lastUpdated = System.currentTimeMillis();
        for (Build build : this.builds) {
            if (build.ref.equals(buildRef)) {
                build.fail(step);
                return;
            }
        }
        throw new UnknownBuild();
    }

    public void cancel(String buildRef) {
        lastUpdated = System.currentTimeMillis();
        for (Build build : this.builds) {
            if (build.ref.equals(buildRef)) {
                build.cancel();
                return;
            }
        }
        throw new UnknownBuild();
    }

    public void verifyIP(String ipAddress) {
        if (ips.length > 0) {
            for (String ip : ips) {
                if (ip.equals(ipAddress)) {
                    return;
                }
            }
            throw new IpNotAuthorized(ipAddress);
        }
    }

    public Radiator withoutSecret() {
        Radiator rClone = new Radiator(this.code, null, this.stepNames).withIpAccessRestrictedToThese(this.ips);
        rClone.lastUpdated = this.lastUpdated;
        rClone.builds.addAll(this.builds);
        return rClone;
    }

    public Radiator verifySecret(String secret) {
        if (!this.secret.equals(secret) || NO_UPDATES.equals(secret)) {
            throw new SecretDoesntMatch();
        }
        return this;
    }

    public CreatedRadiator codeAndSecretOnly() {
        CreatedRadiator cr = new CreatedRadiator();
        cr.code = code;
        cr.secret = secret;
        return cr;
    }
}
