package federates.shop;

import entity.Client;
import entity.Shop;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;

import events.ExternalEvent;
import utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShopFederate {
    private static final Logger logger = new Logger("ShopFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private ShopFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    //Published Interactions
    protected InteractionClassHandle openCashQueueInteractionHandle;
    protected ParameterHandle cashQueueIdOpenCashQueueHandle;

    protected InteractionClassHandle closeCashQueueInteractionHandle;
    protected ParameterHandle cashQueueIdCloseCashQueueHandle;

    protected InteractionClassHandle clientToCashQueueInteractionHandle;
    protected ParameterHandle clientIdClientToCashQueueHandle;
    protected ParameterHandle totalProductsClientToCashQueueHandle;

    protected InteractionClassHandle shopStatsInteractionHandle;
    protected ParameterHandle totalClientsInShopStatsHandle;
    protected ParameterHandle totalClientsInShoppingStatsHandle;
    protected ParameterHandle totalClientsInCashQueuesStatsHandle;
    protected ParameterHandle totalClientsServicedStatsHandle;

    //Subscribed Interactions
    protected InteractionClassHandle clientEnteredShopInteractionHandle;
    protected ParameterHandle clientIdEnteredHandle;

    protected InteractionClassHandle shoppingFinishedInteractionHandle;
    protected ParameterHandle clientIdShoppingFinishedHandle;

    protected InteractionClassHandle stopSimulationInteractionHandle;

    private boolean stopSimulation = false;
    private boolean isInitialCashQueueExists = false;
    private int totalClientsServiced = 0;

    /////////////////////////////////////////////////////////////////////////

    private double randomTime() {
        Random r = new Random();
        return 1 + (4 * r.nextDouble());
    }

    private void run() throws RTIexception {
        while (!stopSimulation) {
            Shop shop = Shop.getInstance();
            logger.info(String.format("[ShopFederate] Total clients in shop: %s, clients in queues: %s, clients in shopping: %s",
                    shop.getAllClientsInShopSum(),
                    shop.getClientsInCashQueueCount(),
                    shop.getAllClientsInShoppingSum()));
            boolean isClientsInShop = shop.getAllClientsInShopSum() > 0;

            advanceTime(Constants.TIME_STEP);
            if (Constants.LOG_TIME_ADVANCE) logger.info(String.format("Time Advanced to %.1f", fedamb.federateTime));

            if (fedamb.getExternalEvents().size() > 0) {
                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                    switch (externalEvent.getEventType()) {
                        case CLIENT_ENTERED_SHOP:
                            if (Shop.getInstance().getAllClientsInShopSum() < Constants.MAX_CLIENTS_IN_SHOP) {
                                String clientIdEnteredShop = (String) externalEvent.getData();
                                clientEnteredReceived(clientIdEnteredShop);
                            }
                            break;
                        case SHOPPING_FINISHED:
                            String clientIdShoppingFinished = (String) externalEvent.getData();
                            shoppingFinishedInteractionReceived(clientIdShoppingFinished);
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }

            if (isClientsInShop) {
                Random rand = new Random();
                List<String> clientsInShoppingIds = new ArrayList<>(shop.getAllClientsInShopping().keySet());

                if (RandomUtils.getRandomBooleanWithProbability(Constants.FINISH_SHOPPING_PROBABILITY) && clientsInShoppingIds.size() > 0) {
                    String randomClientId = clientsInShoppingIds.get(rand.nextInt(clientsInShoppingIds.size()));
                    logger.info(String.format("Client with id: %s joined to queue", randomClientId));
                    Shop.getInstance().getAllClientsInCashQueues().put(randomClientId, Shop.getInstance().getAllClientsInShop().get(randomClientId));
                    Shop.getInstance().getAllClientsInShopping().remove(randomClientId);

                    if (!isInitialCashQueueExists) {
                        String cashQueueId = UUIDUtils.shortId();
                        openCashQueueInteraction(cashQueueId);
                        isInitialCashQueueExists = true;
                    }

                    clientToCashQueueInteraction(randomClientId, Shop.getInstance().getAllClientsInShop().get(randomClientId).getProductsCount());
                }
            }

            sendStats();
        }
    }

    private void shoppingFinishedInteractionReceived(String clientId) {
        Shop.getInstance().getAllClientsInShopping().remove(clientId);
        Shop.getInstance().getAllClientsInCashQueues().remove(clientId);
        Shop.getInstance().getAllClientsInShop().remove(clientId);

        totalClientsServiced++;

        logger.info(String.format("[shoppingFinishedInteractionReceived] Client %s finished shopping and leaved shop.", clientId));
    }

    private void clientEnteredReceived(String klientId) {
        logger.info(String.format("[clientEnteredReceived]: Client with id: %s, entered to shop.", klientId));
        Random rand = new Random();
        float probability = Constants.CLIENTS_PRIVILEGED_PERCENTAGE / 100f;

        Client client = new Client(klientId);

        if (RandomUtils.getRandomBooleanWithProbability(probability)) {
            client.setProductsCount(rand.nextInt(Constants.MAX_TOTAL_PRODUCTS_COUNT + 1) + 5);
        } else {
            client.setProductsCount(rand.nextInt(5) + 1);
        }

        logger.info(String.format("[clientEnteredReceived] Client created: %s", client.toString()));

        Shop.getInstance().getAllClientsInShop().put(client.getClientId(), client);
        Shop.getInstance().getAllClientsInShopping().put(client.getClientId(), client);
    }

    private void publishAndSubscribe() throws RTIexception {
        //PUBLISHED
        openCashQueueInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OpenCashQueue");
        cashQueueIdOpenCashQueueHandle = rtiamb.getParameterHandle(openCashQueueInteractionHandle, "cashQueueId");

        closeCashQueueInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CloseCashQueue");
        cashQueueIdCloseCashQueueHandle = rtiamb.getParameterHandle(closeCashQueueInteractionHandle, "cashQueueId");

        clientToCashQueueInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ClientToCashQueue");
        totalProductsClientToCashQueueHandle = rtiamb.getParameterHandle(clientToCashQueueInteractionHandle, "totalProductsCount");
        clientIdClientToCashQueueHandle = rtiamb.getParameterHandle(clientToCashQueueInteractionHandle, "clientId");

        shopStatsInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ShopStats");
        totalClientsInShopStatsHandle = rtiamb.getParameterHandle(shopStatsInteractionHandle, "totalClientsInShop");
        totalClientsInShoppingStatsHandle = rtiamb.getParameterHandle(shopStatsInteractionHandle, "totalClientsInShopping");
        totalClientsInCashQueuesStatsHandle = rtiamb.getParameterHandle(shopStatsInteractionHandle, "totalClientsInCashQueues");
        totalClientsServicedStatsHandle = rtiamb.getParameterHandle(shopStatsInteractionHandle, "totalClientsServiced");

        //SUBSCRIBED
        clientEnteredShopInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ClientEnteredShop");
        clientIdEnteredHandle = rtiamb.getParameterHandle(clientEnteredShopInteractionHandle, "clientId");

        shoppingFinishedInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ShoppingFinished");
        clientIdShoppingFinishedHandle = rtiamb.getParameterHandle(shoppingFinishedInteractionHandle, "clientId");
        //////////////////////////////////////////////////////////////

        rtiamb.subscribeInteractionClass(clientEnteredShopInteractionHandle);
        rtiamb.subscribeInteractionClass(shoppingFinishedInteractionHandle);

        rtiamb.publishInteractionClass(openCashQueueInteractionHandle);
        rtiamb.publishInteractionClass(closeCashQueueInteractionHandle);
        rtiamb.publishInteractionClass(clientToCashQueueInteractionHandle);
        rtiamb.publishInteractionClass(shopStatsInteractionHandle);
    }

    private void clientToCashQueueInteraction(String clientId, int totalProductsCount) throws RTIexception {
        HLAunicodeString clientIdValue = encoder.createHLAunicodeString(clientId);
        HLAinteger32BE totalProductsCountValue = encoder.createHLAinteger32BE(totalProductsCount);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(clientIdClientToCashQueueHandle, clientIdValue.toByteArray());
        parameters.put(totalProductsClientToCashQueueHandle, totalProductsCountValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[clientToCashQueueInteraction] Send interaction, clientId: %s, [TIME: %.1f]", clientId, newTimeDouble));

        rtiamb.sendInteraction(clientToCashQueueInteractionHandle, parameters, generateTag(), time);
    }

    private void openCashQueueInteraction(String cashQueueId) throws RTIexception {
        HLAunicodeString cashQueueIdValue = encoder.createHLAunicodeString(cashQueueId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(cashQueueIdOpenCashQueueHandle, cashQueueIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[openCashQueueInteraction] Send interaction, cashQueueId: %s [TIME: %.1f]", cashQueueId, newTimeDouble));

        rtiamb.sendInteraction(openCashQueueInteractionHandle, parameters, generateTag(), time);
    }

    private void sendStats() throws RTIexception {
        Shop shop = Shop.getInstance();

        HLAinteger32BE totalClientsInShopValue = encoder.createHLAinteger32BE(shop.getAllClientsInShopSum());
        HLAinteger32BE totalClientsInShoppingValue = encoder.createHLAinteger32BE(shop.getAllClientsInShoppingSum());
        HLAinteger32BE totalClientsInCashQueuesValue = encoder.createHLAinteger32BE(shop.getClientsInCashQueueCount());
        HLAinteger32BE totalClientsServicedValue = encoder.createHLAinteger32BE(this.totalClientsServiced);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(totalClientsInShopStatsHandle, totalClientsInShopValue.toByteArray());
        parameters.put(totalClientsInShoppingStatsHandle, totalClientsInShoppingValue.toByteArray());
        parameters.put(totalClientsInCashQueuesStatsHandle, totalClientsInCashQueuesValue.toByteArray());
        parameters.put(totalClientsServicedStatsHandle, totalClientsServicedValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[StatsInteraction] Latest statistics sent [TIME: %.1f]", newTimeDouble));

        rtiamb.sendInteraction(shopStatsInteractionHandle, parameters, generateTag(), time);
    }

    private void closeCashQueueInteraction(String cashQueueId) throws RTIexception {
        HLAunicodeString cashQueueIdValue = encoder.createHLAunicodeString(cashQueueId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(cashQueueIdCloseCashQueueHandle, cashQueueIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[closeCashQueueInteraction] Send interaction, cashQueueId: %s [TIME: %.1f]", cashQueueId, newTimeDouble));

        rtiamb.sendInteraction(closeCashQueueInteractionHandle, parameters, generateTag(), time);
    }

    private void waitForUser() {
        logger.info(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            reader.readLine();
        } catch (Exception e) {
            logger.info("Error while waiting for user input: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runFederate(String federateName) throws Exception {
        // create rti
        logger.info("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoder = new EncoderDecoder();

        // connect
        logger.info("Connecting...");
        fedamb = new ShopFederateAmbassador(this);
        rtiamb.connect(fedamb, CallbackModel.HLA_EVOKED);

        // create federation
        logger.info("Creating Federation...");
        try {
            URL[] modules = new URL[]{
                    (new File("HLAstandardMIM.xml")).toURI().toURL(),
                    (new File("fom.xml")).toURI().toURL()
            };

            rtiamb.createFederationExecution("SupermarketFederation", modules);
            logger.info("Created Federation");
        } catch (FederationExecutionAlreadyExists exists) {
            logger.info("Didn't create federation, it already existed");
        } catch (MalformedURLException urle) {
            logger.info("Exception loading one of the FOM modules from disk: " + urle.getMessage());
            urle.printStackTrace();
            return;
        }

        // join the federation
        URL[] joinModules = new URL[]{
                (new File("fom.xml")).toURI().toURL()
        };

        rtiamb.joinFederationExecution(federateName,
                "ShopFederateType",
                "SupermarketFederation",
                joinModules);

        logger.info("Joined Federation as " + federateName);


        //announce the sync point
        rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        while (!fedamb.isAnnounced) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        waitForUser();

        //achieve the point and wait for synchronization
        rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        logger.info("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
        while (!fedamb.isReadyToRun) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        //enable time policies
        enableTimePolicy();
        logger.info("Time Policy Enabled");

        //publish and subscribe
        publishAndSubscribe();
        logger.info("Published and Subscribed");

        run();

        // resign from the federation
        rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
        logger.info("Resigned from Federation");

        //try and destroy the federation
        try {
            rtiamb.destroyFederationExecution("SkiStationFederation");
            logger.info("Destroyed Federation");
        } catch (FederationExecutionDoesNotExist dne) {
            logger.info("No need to destroy federation, it doesn't exist");
        } catch (FederatesCurrentlyJoined fcj) {
            logger.info("Didn't destroy federation, federates still joined");
        }
    }

    private void enableTimePolicy() throws Exception {
        LogicalTimeInterval lookahead = TimeUtils.convertInterval(fedamb.federateLookahead);

        this.rtiamb.enableTimeRegulation(lookahead);
        while (!fedamb.isRegulating) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        this.rtiamb.enableTimeConstrained();
        while (!fedamb.isConstrained) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void advanceTime(double timestep) throws RTIexception {
        fedamb.isAdvancing = true;
        double timeToAdvance = fedamb.federateTime + timestep;
        LogicalTime newTime = TimeUtils.convertTime(timeToAdvance);
        if (Constants.LOG_TIME_REQUEST) logger.info("Requesting time advance for: " + timeToAdvance);

        rtiamb.timeAdvanceRequest(newTime);
        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    private void endSimulation() {
        stopSimulation = true;
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main(String[] args) {
        String shopFederateName = "ShopFederate";
        if (args.length != 0) {
            shopFederateName = args[0];
        }

        try {
            ShopFederate federate = new ShopFederate();
            federate.runFederate(shopFederateName);
        } catch (Exception rtie) {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
