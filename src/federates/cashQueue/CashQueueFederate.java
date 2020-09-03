package federates.cashQueue;

import entity.AppInterface;
import entity.Client;
import entity.CashQueue;
import events.ExternalEvent;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class CashQueueFederate {
    private static final Logger logger = new Logger("CashQueueFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private CashQueueFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    // Interactions Published
    protected InteractionClassHandle openCashRegisterInteractionHandle;
    protected ParameterHandle cashRegisterIdOpenCashRegisterHandle;
    protected ParameterHandle privilegedOpenCashRegisterHandle;
    protected ParameterHandle serviceTimeCashRegisterHandle;

    protected InteractionClassHandle closeCashRegisterInteractionHandle;
    protected ParameterHandle cashRegisterIdCloseCashRegisterHandle;

    protected InteractionClassHandle clientToCashRegisterInteractionHandle;
    protected ParameterHandle clientIdToCashRegisterHandle;
    protected ParameterHandle cashRegisterIdCashRegisterHandle;
    protected ParameterHandle productsCountCashRegisterHandle;

    protected InteractionClassHandle cashQueueStatsInteractionHandle;
    protected ParameterHandle clientsCashQueueStatsHandle;
    protected ParameterHandle queuesCountCashQueuesStatsHandle;
    protected ParameterHandle privilegedCashQueuesStatsHandle;

    // Interactions Subscribed
    protected InteractionClassHandle openCashQueueInteractionHandle;
    protected ParameterHandle cashQueueIdOpenCashQueueHandle;

    protected InteractionClassHandle closeCashQueueInteractionHandle;
    protected ParameterHandle cashQueueIdCloseCashQueueHandle;

    protected InteractionClassHandle clientToCashQueueInteractionHandle;
    protected ParameterHandle clientIdClientToCashQueueHandle;
    protected ParameterHandle productsCountClientToCashQueueHandle;

    protected InteractionClassHandle shoppingFinishedInteractionHandle;
    protected ParameterHandle clientIdShoppingFinishedHandle;
    protected ParameterHandle cashRegisterIdShoppingFinishedHandle;
    /////////////////////////////////////////////////

    private boolean isShopOpen = true;
    private String cashQueuesVisualisation = "";

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

    private void runFederate(String federateName) throws Exception {
        // create rti
        logger.info("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoder = new EncoderDecoder();

        // connect
        logger.info("Connecting...");
        fedamb = new CashQueueFederateAmbassador(this);
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
                "CashQueueFederate",
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
            rtiamb.destroyFederationExecution("SupermarketFederation");
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

    private double randomTime() {
        Random r = new Random();
        return 1 + (4 * r.nextDouble());
    }

    private byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    ///////////////////////////////////////////////////////////////////

    public void run() throws RTIexception {
        while (isShopOpen) {
            advanceTime(randomTime());
            if (Constants.LOG_TIME_ADVANCE) logger.info(String.format("Time Advanced to %.1f", fedamb.federateTime));

            if (fedamb.getExternalEvents().size() > 0) {
                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                    switch (externalEvent.getEventType()) {
                        case OPEN_CASH_QUEUE:
                            String cashQueueIdOpenCashQueue = (String) externalEvent.getData();
                            openCashQueueReceived(cashQueueIdOpenCashQueue);
                            break;
                        case CLOSE_CASH_QUEUE:
                            String cashQueueIdCloseCashQueue = (String) externalEvent.getData();
                            closeCashQueueReceived(cashQueueIdCloseCashQueue);
                            break;
                        case CLIENT_TO_QUEUE:
                            Object[] dataClientToQueue = (Object[]) externalEvent.getData();
                            clientToCashQueueReceived(dataClientToQueue);
                            break;
                        case SHOPPING_FINISHED:
                            Object[] dataShoppingFinished = (Object[]) externalEvent.getData();
                            shoppingFinishedInteractionReceived(dataShoppingFinished);
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }

            if (AppInterface.getInstance().getAllQueues().size() > 0) {
                Random rand = new Random();
                Object[] kolejkiIds = AppInterface.getInstance().getAllQueues().keySet().toArray();
                Object key = kolejkiIds[rand.nextInt(kolejkiIds.length)];
                CashQueue wylosowanaCashQueue = AppInterface.getInstance().getAllQueues().get(key);

                if (!isAnyClientInCashout(wylosowanaCashQueue)) {
                    Client pierwszyClientWKolejce = wylosowanaCashQueue.getClients().peek();
                    wylosowanaCashQueue.getClients().peek().setInCashRegister(true);
                    AppInterface.getInstance().getAllQueues().put(wylosowanaCashQueue.getQueueId(), wylosowanaCashQueue);
                    logger.info(String.format("[ClientToCashRegister] First clent in CashQueue: %s", pierwszyClientWKolejce));

                    clientToCashRegisterInteraction(wylosowanaCashQueue.getCashRegisterId(), pierwszyClientWKolejce.getClientId(), pierwszyClientWKolejce.getProductsCount());
                }

                ////////////// STATS PRINT
                List<Integer> dlugosciWszystkichKolejek = new ArrayList<>();
                for (Map.Entry<String, CashQueue> q : AppInterface.getInstance().getAllQueues().entrySet()) {
                    dlugosciWszystkichKolejek.add(q.getValue().getCashQueueLength());
                }

                cashQueuesVisualisation = dlugosciWszystkichKolejek.toString();
                logger.info(String.format("[CashQueuesStatus] All queues length: %s", dlugosciWszystkichKolejek));
            }

            sendStats();
        }
    }

    private boolean isAnyClientInCashout(CashQueue cashQueue) {
        boolean inCashout = false;

        for (Client client : cashQueue.getClients()) {
            if (client.isInCashRegister()) {
                inCashout = true;
            }
        }

        return inCashout;
    }

    private void shoppingFinishedInteractionReceived(Object[] data) throws RTIexception {
        String clientId = (String) data[0];
        String cashRegisterId = (String) data[1];
        AppInterface.getInstance().getAllQueues().get(cashRegisterId).getClients().remove();

        logger.info(String.format("[ShoppingFinished] Client %s removed from cash queue %s", clientId, cashRegisterId));
        if (AppInterface.getInstance().getAllQueues().get(cashRegisterId).getClients().size() == 0) {
            logger.info(String.format("[ShoppingFinished] No clients to cash register %s. Sending close cash register interaction", cashRegisterId));
            AppInterface.getInstance().getAllQueues().remove(cashRegisterId);
            closeCashRegisterInteraction(cashRegisterId);
        }

    }

    private void openCashQueueReceived(String cashQueueId) {
        Random rand = new Random();

        CashQueue cashQueue = new CashQueue();
        cashQueue.setQueueId(cashQueueId);
        cashQueue.setCashRegisterId(cashQueueId);
        cashQueue.setAverageServiceTime(rand.nextInt(Constants.MAX_AVERAGE_SERVICE_TIME + 1) + 1);
        cashQueue.setClients(new LinkedList<>());
        cashQueue.setPrivileged(RandomUtils.getRandomBooleanWithProbability(Constants.OPEN_PRIVILEGED_CASH_REGISTER_PROBABILITY));

        AppInterface.getInstance().getAllQueues().put(cashQueue.getQueueId(), cashQueue);

        try {
            openCashRegisterInteraction(cashQueueId, cashQueue.isPrivileged(), cashQueue.getAverageServiceTime());
        } catch (RTIexception e) {
            logger.info("[OpenCashRegister] RTIException, unable to open cash register");
        }

        logger.info(String.format("Opened cash queue %s, opened cash queues count: %s", cashQueueId, AppInterface.getInstance().getAllQueues().size()));
    }

    private void closeCashQueueReceived(String cashQueueId) {

    }

    private void clientToCashQueueReceived(Object[] data) {
        String clientId = (String) data[0];
        int totalProductsCount = (int) data[1];
        Client client = new Client(clientId, totalProductsCount, false);
        CashQueue cashQueue = client.getProductsCount() <= 5
                ? AppInterface.getInstance().getShortestPrivilegedQueue()
                : AppInterface.getInstance().getShortestQueue();

        if (cashQueue == null) {
            String newCashQueueId = UUIDUtils.shortId();
            openCashQueueReceived(newCashQueueId);
            cashQueue = AppInterface.getInstance().getAllQueues().get(newCashQueueId);
        }

        if (cashQueue.getCashQueueLength() >= Constants.MAX_CASH_QUEUE_LENGTH) {
            String newCashQueueId = UUIDUtils.shortId();
            openCashQueueReceived(newCashQueueId);
            cashQueue = AppInterface.getInstance().getShortestQueue();
        }

        cashQueue.getClients().add(client);
        AppInterface.getInstance().getAllQueues().put(cashQueue.getQueueId(), cashQueue);

        logger.info(String.format("[ClientToCashQueueReceived] Client %s joined to cash queue %s. Total clients in cash queue count: %s",
                client.getClientId(),
                cashQueue.getQueueId(),
                cashQueue.getCashQueueLength()));
    }

    private void publishAndSubscribe() throws RTIexception {
        // PUBLISHED
        openCashRegisterInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OpenCashRegister");
        cashRegisterIdOpenCashRegisterHandle = rtiamb.getParameterHandle(openCashRegisterInteractionHandle, "cashRegisterId");
        privilegedOpenCashRegisterHandle = rtiamb.getParameterHandle(openCashRegisterInteractionHandle, "privileged");
        serviceTimeCashRegisterHandle = rtiamb.getParameterHandle(openCashRegisterInteractionHandle, "serviceTime");

        closeCashRegisterInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CloseCashRegister");
        cashRegisterIdCloseCashRegisterHandle = rtiamb.getParameterHandle(closeCashRegisterInteractionHandle, "cashRegisterId");

        clientToCashRegisterInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ClientToCashRegister");
        clientIdToCashRegisterHandle = rtiamb.getParameterHandle(clientToCashRegisterInteractionHandle, "clientId");
        cashRegisterIdCashRegisterHandle = rtiamb.getParameterHandle(clientToCashRegisterInteractionHandle, "cashRegisterId");
        productsCountCashRegisterHandle = rtiamb.getParameterHandle(clientToCashRegisterInteractionHandle, "totalProductsCount");

        cashQueueStatsInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CashQueueStats");
        clientsCashQueueStatsHandle = rtiamb.getParameterHandle(cashQueueStatsInteractionHandle, "clientsInCashQueues");
        queuesCountCashQueuesStatsHandle = rtiamb.getParameterHandle(cashQueueStatsInteractionHandle, "totalCashQueuesCount");
        privilegedCashQueuesStatsHandle = rtiamb.getParameterHandle(cashQueueStatsInteractionHandle, "totalCashQueuesPrivilegedCount");

        // SUBSCRIBED
        openCashQueueInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OpenCashQueue");
        cashQueueIdOpenCashQueueHandle = rtiamb.getParameterHandle(openCashQueueInteractionHandle, "cashQueueId");

        closeCashQueueInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CloseCashQueue");
        cashQueueIdCloseCashQueueHandle = rtiamb.getParameterHandle(closeCashQueueInteractionHandle, "cashQueueId");

        shoppingFinishedInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ShoppingFinished");
        clientIdShoppingFinishedHandle = rtiamb.getParameterHandle(shoppingFinishedInteractionHandle, "clientId");
        cashRegisterIdShoppingFinishedHandle = rtiamb.getParameterHandle(shoppingFinishedInteractionHandle, "cashRegisterId");

        clientToCashQueueInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ClientToCashQueue");
        productsCountClientToCashQueueHandle = rtiamb.getParameterHandle(clientToCashQueueInteractionHandle, "totalProductsCount");
        clientIdClientToCashQueueHandle = rtiamb.getParameterHandle(clientToCashQueueInteractionHandle, "clientId");
        ////////////////////////////////////////

        rtiamb.publishInteractionClass(openCashRegisterInteractionHandle);
        rtiamb.publishInteractionClass(closeCashRegisterInteractionHandle);
        rtiamb.publishInteractionClass(clientToCashRegisterInteractionHandle);
        rtiamb.publishInteractionClass(cashQueueStatsInteractionHandle);

        rtiamb.subscribeInteractionClass(openCashQueueInteractionHandle);
        rtiamb.subscribeInteractionClass(shoppingFinishedInteractionHandle);
        rtiamb.subscribeInteractionClass(closeCashRegisterInteractionHandle);
        rtiamb.subscribeInteractionClass(clientToCashQueueInteractionHandle);
    }

    private void openCashRegisterInteraction(String cashRegisterId, boolean privileged, int serviceTime) throws RTIexception {
        HLAunicodeString cashRegisterIdValue = encoder.createHLAunicodeString(cashRegisterId);
        HLAboolean privilegedValue = encoder.createHLAboolean(privileged);
        HLAinteger32BE serviceTimeValue = encoder.createHLAinteger32BE(serviceTime);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(cashRegisterIdOpenCashRegisterHandle, cashRegisterIdValue.toByteArray());
        parameters.put(privilegedOpenCashRegisterHandle, privilegedValue.toByteArray());
        parameters.put(serviceTimeCashRegisterHandle, serviceTimeValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[OpenCashRegisterInteraction] Send interaction, cashRegisterId and cashQueueId: %s [TIME: %.1f]", cashRegisterId, newTimeDouble));

        rtiamb.sendInteraction(openCashRegisterInteractionHandle, parameters, generateTag(), time);
    }

    private void closeCashRegisterInteraction(String cashRegisterId) throws RTIexception {
        HLAunicodeString cashRegisterIdValue = encoder.createHLAunicodeString(cashRegisterId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(cashRegisterIdCloseCashRegisterHandle, cashRegisterIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[CloseCashRegisterInteraction] Send interaction, cashRegisterId and cashQueueId: %s [TIME: %.1f]", cashRegisterId, newTimeDouble));

        rtiamb.sendInteraction(closeCashRegisterInteractionHandle, parameters, generateTag(), time);
    }

    private void sendStats() throws RTIexception {
        HLAunicodeString cashQueueVisualisationValue = encoder.createHLAunicodeString(this.cashQueuesVisualisation);
        HLAinteger32BE cashQueuesCountValue = encoder.createHLAinteger32BE(AppInterface.getInstance().getAllQueues().size());
        HLAinteger32BE cashQueuesPrivilegedCountValue = encoder.createHLAinteger32BE(AppInterface.getInstance().getPrivilegedQueuesCount());

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(clientsCashQueueStatsHandle, cashQueueVisualisationValue.toByteArray());
        parameters.put(queuesCountCashQueuesStatsHandle, cashQueuesCountValue.toByteArray());
        parameters.put(privilegedCashQueuesStatsHandle, cashQueuesPrivilegedCountValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[SendStats] Send latest statistics [TIME: %.1f]", newTimeDouble));

        rtiamb.sendInteraction(cashQueueStatsInteractionHandle, parameters, generateTag(), time);
    }

    private void clientToCashRegisterInteraction(String cashRegisterId, String clientId, int productsCount) throws RTIexception {
        HLAunicodeString cashRegisterIdValue = encoder.createHLAunicodeString(cashRegisterId);
        HLAunicodeString clientIdValue = encoder.createHLAunicodeString(clientId);
        HLAinteger32BE productsCountValue = encoder.createHLAinteger32BE(productsCount);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(cashRegisterIdCashRegisterHandle, cashRegisterIdValue.toByteArray());
        parameters.put(clientIdToCashRegisterHandle, clientIdValue.toByteArray());
        parameters.put(productsCountCashRegisterHandle, productsCountValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[ClientToCashRegisterInteraction] Send interaction, cashRegisterId: %s and clientId: %s [TIME: %.1f]", cashRegisterId, clientId, newTimeDouble));

        rtiamb.sendInteraction(clientToCashRegisterInteractionHandle, parameters, generateTag(), time);
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main(String[] args) {
        String federateName = "CashQueueFederate";
        if (args.length != 0) {
            federateName = args[0];
        }

        try {
            new CashQueueFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }
}
