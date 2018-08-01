package com.paulhammant.buildradiator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.InetAddresses;
import com.google.datastore.v1.*;
import com.google.datastore.v1.client.Datastore;
import com.google.datastore.v1.client.DatastoreException;
import com.google.datastore.v1.client.DatastoreFactory;
import com.google.datastore.v1.client.DatastoreHelper;
import com.google.protobuf.ByteString;
import com.paulhammant.buildradiator.model.NotAnIPAddress;
import com.paulhammant.buildradiator.model.Radiator;
import com.paulhammant.buildradiator.model.RadiatorDoesntExist;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RadiatorStore {

    final Map<String, Radiator> actualRadiators = new ConcurrentHashMap<>();
    private final Map<String, Long> radiatorLastSavedTimes = new ConcurrentHashMap<>();
    private boolean saverShouldKeepGoing = true;

    RadiatorStore() {
        this(false);
    }
    RadiatorStore(boolean onAppEngine) {
        Thread saver = new Thread(() -> {
            while (saverShouldKeepGoing) {
                processRadiatorsToSave();
                try {
                    Thread.sleep(getMillisToDelay());
                } catch (InterruptedException e) {
                }
            }
        });
        saver.setDaemon(true);
        saver.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (onAppEngine) {
                System.out.println("Shutdown detectected .. ");
            }
            processRadiatorsToSave();
            if (onAppEngine) {
                System.out.println(".. ready for shutdown");
            }
        }));
    }

    protected int getMillisToDelay() {
        return 5 * 60 * 1000;
    }

    private void processRadiatorsToSave() {
        synchronized (radiatorLastSavedTimes) {
            Set<String> ks = actualRadiators.keySet();
            for (String radCode : ks) {
                Radiator rad = actualRadiators.get(radCode);
                Long lastSaved = radiatorLastSavedTimes.get(radCode);
                if (lastSaved == null || rad.lastUpdated > lastSaved) {
                    saveInDataService(radCode, rad);
                    radiatorLastSavedTimes.put(radCode, rad.lastUpdated);
                }
            }
        }
    }

    Radiator get(final String radCode, String ipAddress) {
        Radiator radiator = this.actualRadiators.get(radCode);
        if (radiator == null) {
            radiator = getFromDataService(radCode);
            if (radiator != null) {
                actualRadiators.put(radCode, radiator);
                radiatorLastSavedTimes.put(radCode, radiator.lastUpdated);
            }
        }
        if (radiator != null) {
            radiator.verifyIP(ipAddress);
        } else {
            throw new RadiatorDoesntExist();
        }
        return radiator;
    }

    protected Radiator getFromDataService(String radCode) {
        return null;
    }

    protected void saveInDataService(String radCode, Radiator rad) {
        radiatorLastSavedTimes.put(radCode, rad.lastUpdated);
    }

    Radiator createRadiator(RandomGenerator codeGenerator, String... steps) {
        String radiatorCode = codeGenerator.generateRadiatorCode();
        String secret = codeGenerator.generateSecret();
        Radiator radiator = new Radiator(radiatorCode, secret, steps);
        this.actualRadiators.put(radiatorCode, radiator);
        return radiator;
    }

    public void stopSaver() {
        saverShouldKeepGoing = false;
    }

    public static class BackedByGoogleCloudDataStore extends RadiatorStore {

        private Datastore datastore;
        private ObjectMapper om = new ObjectMapper();

        public BackedByGoogleCloudDataStore() {
            super(true);

            // Set the project ID from the command line parameters.
            String projectId = System.getenv("GCLOUD_PROJECT");

            // Setup the connection to Google Cloud Datastore and infer credentials
            // from the environment.
            try {
                datastore = DatastoreFactory.get().create(DatastoreHelper.getOptionsFromEnv()
                        .projectId(projectId).build());
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected Radiator getFromDataService(String radCode) {
            try {
                // Create an RPC request to begin a new transaction.
                BeginTransactionRequest.Builder treq = BeginTransactionRequest.newBuilder();
                // Execute the RPC synchronously.
                BeginTransactionResponse tres = datastore.beginTransaction(treq.build());
                // Get the transaction handle from the response.
                ByteString tx = tres.getTransaction();

                // Create an RPC request to get entities by key.
                LookupRequest.Builder lreq = LookupRequest.newBuilder();
                // Set the entity key with only one `path`: no parent.
                Key.Builder key = Key.newBuilder().addPath(
                        Key.PathElement.newBuilder()
                                .setKind("Radiator")
                                .setName(radCode));
                // Add one key to the lookup request.
                lreq.addKeys(key);
                // Set the transaction, so we get a consistent snapshot of the
                // entity at the time the transaction started.
                lreq.getReadOptionsBuilder().setTransaction(tx);
                // Execute the RPC and get the response.
                LookupResponse lresp = datastore.lookup(lreq.build());
                // Create an RPC request to commit the transaction.
                CommitRequest.Builder creq = CommitRequest.newBuilder();
                // Set the transaction to commit.
                creq.setTransaction(tx);
                Entity entity;
                if (lresp.getFoundCount() > 0) {
                    entity = lresp.getFound(0).getEntity();
                } else {
                    return null;
                }

                return om.readValue((String) entity.getProperties().get("rad").getStringValue(), Radiator.class);

            } catch (DatastoreException e) {
                // Catch all Datastore rpc errors.
                System.err.println("Error while doing datastore operation");
                // Log the exception, the name of the method called and the error code.
                System.err.println(String.format("DatastoreException(%s): %s %s",
                        e.getMessage(),
                        e.getMethodName(),
                        e.getCode()));
            } catch (IOException e) {
                System.err.println("Error while doing objectMapper operation");
                System.err.println(e.getMessage());
            }

            return null;
        }

        @Override
        protected void saveInDataService(String radCode, Radiator rad) {

            try {
                BeginTransactionRequest.Builder treq = BeginTransactionRequest.newBuilder();

                BeginTransactionResponse tres = null;
                tres = datastore.beginTransaction(treq.build());
                // Get the transaction handle from the response.
                ByteString tx = tres.getTransaction();

                CommitRequest.Builder creq = CommitRequest.newBuilder();

                creq.setTransaction(tx);

                Entity entity;

                Key.Builder key = Key.newBuilder().addPath(
                        Key.PathElement.newBuilder()
                                .setKind("Radiator")
                                .setName(radCode));

                LookupRequest.Builder lreq = LookupRequest.newBuilder();
                lreq.addKeys(key);
                // Set the transaction, so we get a consistent snapshot of the
                // entity at the time the transaction started.
                lreq.getReadOptionsBuilder().setTransaction(tx);
                LookupResponse lresp = datastore.lookup(lreq.build());

                // If no entity was found, create a new one.
                Entity.Builder entityBuilder = Entity.newBuilder();
                // Set the entity key.
                entityBuilder.setKey(key);
                // Add two entity properties:
                // - a utf-8 string: `question`

                entityBuilder.getMutableProperties().put("rad", Value.newBuilder()
                        .setExcludeFromIndexes(true)
                        .setStringValue(om.writeValueAsString(rad)).build());
                // Build the entity.
                entity = entityBuilder.build();
                // Insert the entity in the commit request mutation.
                if (lresp.getFoundCount() == 0) {
                    creq.addMutationsBuilder().setInsert(entity);
                } else {
                    creq.addMutationsBuilder().setUpdate(entity);
                }

                datastore.commit(creq.build());
            } catch (DatastoreException | JsonProcessingException e) {
                System.err.println("Error while doing db operation");
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
