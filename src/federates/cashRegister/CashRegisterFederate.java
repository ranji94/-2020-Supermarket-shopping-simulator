package federates.cashRegister;

import entity.AppInterface;
import entity.CashRegister;
import events.ExternalEvent;
import hla.rti1516e.*;
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
import java.util.Random;

public class CashRegisterFederate {
    private static final Logger logger = new Logger("CashRegisterFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private CashRegisterFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    // SUBSCRIBED Interactions
    protected InteractionClassHandle openCashRegisterInteractionHandle;
    protected ParameterHandle cashRegisterIdOpenCashRegisterHandle;
    protected ParameterHandle privilegedOpenCashRegisterHandle;
    protected ParameterHandle serviceTimeOpenCashRegisterHandle;

    protected InteractionClassHandle clientToCashRegisterInteractionHandle;
    protected ParameterHandle clientIdClientToCashRegisterHandle;
    protected ParameterHandle cashRegisterIdClientToCashRegisterHandle;
    protected ParameterHandle totalProductsClientToCashRegisterHandle;

    protected InteractionClassHandle closeCashRegisterInteractionHandle;
    protected ParameterHandle cashRegisterIdCloseCashRegisterHandle;

    // PUBLISHED Interactions
    protected InteractionClassHandle shoppingFinishedInteractionHandle;
    protected ParameterHandle clientIdShoppingFinishedHandle;
    protected ParameterHandle cashRegisterIdShoppingFinishedHandle;

    protected InteractionClassHandle cashRegisterStatsInteractionHandle;
    protected ParameterHandle totalProductsBoughtCashRegisterStatsHandle;
    protected ParameterHandle totalProductsReturnedCashRegisterStatsHandle;
    protected ParameterHandle privilegedCashRegisterStatsHandle;

    ////////////////////////////////////////////////////

    private boolean isShopOpen = true;
    private int totalProductsBought = 0;
    private int totalProductsReturned = 0;
    private int privilegedUsedCount = 0;

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
        fedamb = new CashRegisterFederateAmbassador(this);
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
                "CashRegisterFederate",
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

    private double randomTimeWithParameter(int parameter) {
        Random r = new Random();
        return 1 + (parameter * r.nextDouble());
    }

    private byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    ///////////////////////////////////////////////////////////////////

    public void run() throws RTIexception {
        while (isShopOpen) {
            double timeStep = Constants.TIME_STEP;
            if (fedamb.getExternalEvents().size() > 0) {
                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                    switch (externalEvent.getEventType()) {
                        case OPEN_CASH_REGISTER:
                            Object[] dataOpenCashRegister = (Object[]) externalEvent.getData();
                            openCashRegisterInteractionReceived(dataOpenCashRegister);
                            break;
                        case CLIENT_TO_CASH_REGISTER:
                            Object[] dataClientToCashRegister = (Object[]) externalEvent.getData();
                            String cashRegisterIdClientToCashRegister = (String) dataClientToCashRegister[0];
                            CashRegister cashRegister = AppInterface.getInstance().getAllCashRegisters().get(cashRegisterIdClientToCashRegister);
                            if (cashRegister != null) {
                                int serviceTimeNormalized = Constants.MAX_AVERAGE_SERVICE_TIME + 1 - cashRegister.getServiceTime();
                                timeStep = randomTimeWithParameter(serviceTimeNormalized);
                                clientToCashRegisterInteractionReceived(dataClientToCashRegister);
                            }
                            break;
                        case CLOSE_CASH_REGISTER:
                            String cashRegisterIdCloseCashRegister = (String) externalEvent.getData();
                            closeCashRegisterInteractionReceived(cashRegisterIdCloseCashRegister);
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }

            sendStats();
            advanceTime(timeStep);
            if (Constants.LOG_TIME_ADVANCE) logger.info(String.format("Time Advanced to %.1f", fedamb.federateTime));
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        //PUBLISHED DECLARATIONS
        shoppingFinishedInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ShoppingFinished");
        clientIdShoppingFinishedHandle = rtiamb.getParameterHandle(shoppingFinishedInteractionHandle, "clientId");
        cashRegisterIdShoppingFinishedHandle = rtiamb.getParameterHandle(shoppingFinishedInteractionHandle, "cashRegisterId");

        cashRegisterStatsInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CashRegisterStats");
        totalProductsBoughtCashRegisterStatsHandle = rtiamb.getParameterHandle(cashRegisterStatsInteractionHandle, "totalProductsBought");
        totalProductsReturnedCashRegisterStatsHandle = rtiamb.getParameterHandle(cashRegisterStatsInteractionHandle, "totalProductsReturned");
        privilegedCashRegisterStatsHandle = rtiamb.getParameterHandle(cashRegisterStatsInteractionHandle, "clientsUsedPrivilegedCashRegister");

        // SUBSCRIBED DECLARATIONS
        openCashRegisterInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OpenCashRegister");
        cashRegisterIdOpenCashRegisterHandle = rtiamb.getParameterHandle(openCashRegisterInteractionHandle, "cashRegisterId");
        privilegedOpenCashRegisterHandle = rtiamb.getParameterHandle(openCashRegisterInteractionHandle, "privileged");
        serviceTimeOpenCashRegisterHandle = rtiamb.getParameterHandle(openCashRegisterInteractionHandle, "serviceTime");

        clientToCashRegisterInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ClientToCashRegister");
        clientIdClientToCashRegisterHandle = rtiamb.getParameterHandle(clientToCashRegisterInteractionHandle, "clientId");
        cashRegisterIdClientToCashRegisterHandle = rtiamb.getParameterHandle(clientToCashRegisterInteractionHandle, "cashRegisterId");
        totalProductsClientToCashRegisterHandle = rtiamb.getParameterHandle(clientToCashRegisterInteractionHandle, "totalProductsCount");

        closeCashRegisterInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CloseCashRegister");
        cashRegisterIdCloseCashRegisterHandle = rtiamb.getParameterHandle(closeCashRegisterInteractionHandle, "cashRegisterId");

        // PUBLISHED
        rtiamb.publishInteractionClass(cashRegisterStatsInteractionHandle);
        rtiamb.publishInteractionClass(shoppingFinishedInteractionHandle);

        // SUBSCRIBED
        rtiamb.subscribeInteractionClass(openCashRegisterInteractionHandle);
        rtiamb.subscribeInteractionClass(clientToCashRegisterInteractionHandle);
        rtiamb.subscribeInteractionClass(closeCashRegisterInteractionHandle);
    }

    private void closeCashRegisterInteractionReceived(String cashRegisterId) {
        logger.info(String.format("[closeCashRegisterInteractionReceived] Closing cash register with id: %s", cashRegisterId));
        AppInterface.getInstance().getAllCashRegisters().remove(cashRegisterId);
    }

    private void openCashRegisterInteractionReceived(Object[] data) {
        String cashRegisterId = (String) data[0];
        boolean privileged = (boolean) data[1];
        int serviceTime = (int) data[2];

        CashRegister cashRegister = new CashRegister(cashRegisterId, cashRegisterId, null, serviceTime, privileged);
        AppInterface.getInstance().getAllCashRegisters().put(cashRegisterId, cashRegister);

        logger.info(String.format("[openCashRegisterInteractionReceived] Opened cash register with id: %s, privileged: %s. Opened cash registers: %s",
                cashRegisterId,
                privileged,
                AppInterface.getInstance().getAllCashRegisters().size()));
    }

    private void clientToCashRegisterInteractionReceived(Object[] data) throws RTIexception {
        String cashRegisterId = (String) data[0];
        String clientId = (String) data[1];
        int totalProductsCount = (int) data[2];
        logger.info(String.format("[clientToCashRegisterInteractionReceived] Data received cashRegisterId: %s, clientId: %s", cashRegisterId, clientId));

        if (RandomUtils.getRandomBooleanWithProbability(Constants.PRODUCT_RETURN_PROBABILITY)) {
            totalProductsReturned += totalProductsCount;
        } else {
            totalProductsBought += totalProductsCount;
        }

        CashRegister cashRegister = AppInterface.getInstance().getAllCashRegisters().get(cashRegisterId);
        cashRegister.setCurrentClientId(clientId);

        if(cashRegister.isPrivileged()) {
            privilegedUsedCount++;
        }

        shoppingFinishedInteraction(clientId, cashRegister.getCashRegisterId());

        logger.info(String.format("[clientToCashRegisterInteractionReceived] Client %s joined to cash register %s. \n Total products boughtw: %s \n Total products returned: %s", clientId, cashRegisterId, totalProductsBought, totalProductsReturned));
    }

    private void shoppingFinishedInteraction(String clientId, String cashRegisterId) throws RTIexception {
        HLAunicodeString clientIdValue = encoder.createHLAunicodeString(clientId);
        HLAunicodeString cashRegisterIdValue = encoder.createHLAunicodeString(cashRegisterId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(clientIdShoppingFinishedHandle, clientIdValue.toByteArray());
        parameters.put(cashRegisterIdShoppingFinishedHandle, cashRegisterIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[shoppingFinishedInteraction] Send interaction clientId: %s [TIME: %.1f]", clientId, newTimeDouble));

        rtiamb.sendInteraction(shoppingFinishedInteractionHandle, parameters, generateTag(), time);
    }

    private void sendStats() throws RTIexception {
        HLAinteger32BE totalProductsBoughtValue = encoder.createHLAinteger32BE(this.totalProductsBought);
        HLAinteger32BE totalProductsReturnedValue = encoder.createHLAinteger32BE(this.totalProductsReturned);
        HLAinteger32BE privilegedUsedCountValue = encoder.createHLAinteger32BE(this.privilegedUsedCount);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(totalProductsBoughtCashRegisterStatsHandle, totalProductsBoughtValue.toByteArray());
        parameters.put(totalProductsReturnedCashRegisterStatsHandle, totalProductsReturnedValue.toByteArray());
        parameters.put(privilegedCashRegisterStatsHandle, privilegedUsedCountValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[sendStats] Latest statistics sent [TIME: %.1f]", newTimeDouble));

        rtiamb.sendInteraction(cashRegisterStatsInteractionHandle, parameters, generateTag(), time);
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main(String[] args) {
        String federateName = "CashRegisterFederate";
        if (args.length != 0) {
            federateName = args[0];
        }

        try {
            new CashRegisterFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }
}
