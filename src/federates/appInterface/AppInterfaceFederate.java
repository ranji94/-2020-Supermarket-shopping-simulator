package federates.appInterface;

import events.ExternalEvent;
import gui.GUIController;
import gui.GUIHandler;
import gui.GUI;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import utils.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class AppInterfaceFederate {
    private static final Logger logger = new Logger("AppInterfaceFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private AppInterfaceFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    // SUBSCRIBED interactions
    protected InteractionClassHandle shopStatsInteractionHandle;
    protected ParameterHandle allClientsInShopHandle;
    protected ParameterHandle clientsShoppingHandle;
    protected ParameterHandle clientsInCashQueuesHandle;
    protected ParameterHandle clientsAfterShoppingCountHandle;

    protected InteractionClassHandle cashQueueStatsInteractionHandle;
    protected ParameterHandle clientsCashQueueStatsHandle;
    protected ParameterHandle queuesCountCashQueueStatsHandle;
    protected ParameterHandle privilegedCashQueueStatsHandle;

    protected InteractionClassHandle cashRegisterStatsInteractionHandle;
    protected ParameterHandle boughtProductsCashRegisterHandle;
    protected ParameterHandle returnedProductsCashRegisterHandle;
    protected ParameterHandle usedPrivilegedCashRegisterHandle;
    //////////////////////////////////////////////////

    protected InteractionClassHandle stopSimulationInteractionHandle;

    private GUIHandler guihandler;

    private boolean isShopOpen = true;

    private int totalClientsInShop = 0;
    private int clientsInCashQueuesSum = 0;
    private int clientsInShoppingSum = 0;
    private int clientsServicedSum = 0;
    private String clientsInQueues = "";
    private int queuesCount = 0;
    private int queuesPrivilegedCount = 0;
    private int boughtProducts = 0;
    private int returnedProducts = 0;
    private int usedPrivilegedSum = 0;
    private int maxCashQueueLength =0;

    private void waitForUser()
    {
        logger.info( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            logger.info( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private void runFederate( String federateName ) throws Exception
    {
        // create rti
        logger.info( "Creating RTIambassador" );
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoder = new EncoderDecoder();

        // connect
        logger.info( "Connecting..." );
        fedamb = new AppInterfaceFederateAmbassador( this );
        rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );

        // create federation
        logger.info( "Creating Federation..." );
        try
        {
            URL[] modules = new URL[]{
                    (new File("HLAstandardMIM.xml")).toURI().toURL(),
                    (new File("fom.xml")).toURI().toURL()
            };

            rtiamb.createFederationExecution( "SupermarketFederation", modules );
            logger.info( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            logger.info( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            logger.info( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }

        // join the federation
        URL[] joinModules = new URL[]{
                (new File("fom.xml")).toURI().toURL()
        };

        rtiamb.joinFederationExecution( federateName,
                "AppInterfaceFederate",
                "SupermarketFederation",
                joinModules );

        logger.info( "Joined Federation as " + federateName );

        //announce the sync point
        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        while(!fedamb.isAnnounced)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        waitForUser();

        //achieve the point and wait for synchronization
        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        logger.info( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while(!fedamb.isReadyToRun)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        //enable time policies
        enableTimePolicy();
        logger.info( "Time Policy Enabled" );

        //publish and subscribe
        publishAndSubscribe();
        logger.info( "Published and Subscribed" );

        run();

        // resign from the federation
        rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
        logger.info( "Resigned from Federation" );

        //try and destroy the federation
        try
        {
            rtiamb.destroyFederationExecution( "SupermarketFederation" );
            logger.info( "Destroyed Federation" );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
            logger.info( "No need to destroy federation, it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
            logger.info( "Didn't destroy federation, federates still joined" );
        }
    }

    private void enableTimePolicy() throws Exception
    {
        LogicalTimeInterval lookahead = TimeUtils.convertInterval(fedamb.federateLookahead);

        this.rtiamb.enableTimeRegulation( lookahead );
        while(!fedamb.isRegulating)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        this.rtiamb.enableTimeConstrained();
        while(!fedamb.isConstrained)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    private void advanceTime( double timestep ) throws RTIexception
    {
        fedamb.isAdvancing = true;
        double timeToAdvance = fedamb.federateTime + timestep;
        LogicalTime newTime = TimeUtils.convertTime(timeToAdvance);
        if (Constants.LOG_TIME_REQUEST) logger.info("Requesting time advance for: " + timeToAdvance);

        rtiamb.timeAdvanceRequest( newTime );
        while( fedamb.isAdvancing )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    private double randomTime() {
        Random r = new Random();
        return 1 +(4 * r.nextDouble());
    }

    private byte[] generateTag()
    {
        return ("(timestamp) "+System.currentTimeMillis()).getBytes();
    }

    ///////////////////////////////////////////////////////////////////

    public void run() throws RTIexception {
        while(isShopOpen) {
            advanceTime(randomTime());
            if (Constants.LOG_TIME_ADVANCE) logger.info(String.format("Time Advanced to %.1f", fedamb.federateTime));

            if (fedamb.getExternalEvents().size() > 0) {
                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                    switch (externalEvent.getEventType()) {
                        case SHOP_STATS:
                            Object[] dataSklep = (Object[]) externalEvent.getData();
                            updateShopStats(dataSklep);
                            break;
                        case CASH_REGISTER_STATS:
                            Object[] dataKasa = (Object[]) externalEvent.getData();
                            updateCashRegisterStats(dataKasa);
                            break;
                        case CASH_QUEUE_STATS:
                            Object[] dataKolejka = (Object[]) externalEvent.getData();
                            updateCashQueueStats(dataKolejka);
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }

            logger.info(String.format("SHOP STATS: \n" +
                    "All clients in shop: %s \n" +
                    "All clients during shopping: %s \n" +
                    "All clients in cash queues: %s \n" +
                    "Clients serviced: %s \n" +
                    "CashQueues visualisation: %s \n" +
                    "Total CashQueues Count: %s \n" +
                    "PrivilegedQueuesOpen: %s \n" +
                    "Products bought: %s \n" +
                    "Products returned: %s \n" +
                    "Clients used privileged cash queue count: %s",
                    totalClientsInShop,
                    clientsInShoppingSum,
                    clientsInCashQueuesSum,
                    clientsServicedSum,
                    clientsInQueues,
                    queuesCount,
                    queuesPrivilegedCount,
                    boughtProducts,
                    returnedProducts,
                    usedPrivilegedSum));
        }

        stopSimulation();
    }

    private void updateShopStats(Object [] data) {
        int allClientsCount = (int) data[0];
        int allClientsInShoppingCount = (int) data[1];
        int allClientsInCashQueuesCount = (int) data[2];
        int allClientsServicedCount = (int) data[3];

        totalClientsInShop = allClientsCount;
        clientsInShoppingSum = allClientsInShoppingCount;
        clientsInCashQueuesSum = allClientsInCashQueuesCount;
        clientsServicedSum = allClientsServicedCount;

        guihandler.addTotalClientsInShop(totalClientsInShop);
        guihandler.addTotalClientsInShopping(clientsInShoppingSum);
        guihandler.addTotalClientsInCashQueues(clientsInCashQueuesSum);
        guihandler.addTotalClientsServiced(clientsServicedSum);
    }

    private void updateCashQueueStats(Object [] data) {
        String allClientsInCashQueuesCount = (String) data[0];
        int totalQueuesSum = (int) data[1];
        int privilegedCashQueuesSum = (int) data[2];

        clientsInQueues = allClientsInCashQueuesCount;
        this.queuesCount = totalQueuesSum;
        queuesPrivilegedCount = privilegedCashQueuesSum;

        if(maxCashQueueLength <totalQueuesSum){
            maxCashQueueLength =totalQueuesSum;
            guihandler.addMaxQueuesCount(maxCashQueueLength);
        }

        guihandler.addCashQueuesCount(totalQueuesSum);
        guihandler.cashQueuesVisualisation(allClientsInCashQueuesCount);
        guihandler.addClientsPrivileged(queuesPrivilegedCount);
    }

    private void updateCashRegisterStats(Object [] data) {
        int productsBought = (int) data[0];
        int productsReturned = (int) data[1];
        int usedPrivilegedCashRegisterCount = (int) data[2];

        this.boughtProducts = productsBought;
        this.returnedProducts = productsReturned;
        usedPrivilegedSum = usedPrivilegedCashRegisterCount;

        guihandler.addTotalProductsBought(productsBought);
        guihandler.addTotalProductsReturned(productsReturned);
        guihandler.addClientsPrivileged(usedPrivilegedSum);
    }

    private void publishAndSubscribe() throws RTIexception
    {
        shopStatsInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ShopStats");
        allClientsInShopHandle = rtiamb.getParameterHandle(shopStatsInteractionHandle, "totalClientsInShop");
        clientsShoppingHandle = rtiamb.getParameterHandle(shopStatsInteractionHandle, "totalClientsInShopping");
        clientsInCashQueuesHandle = rtiamb.getParameterHandle(shopStatsInteractionHandle, "totalClientsInCashQueues");
        clientsAfterShoppingCountHandle = rtiamb.getParameterHandle(shopStatsInteractionHandle, "totalClientsServiced");

        cashQueueStatsInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CashQueueStats");
        clientsCashQueueStatsHandle = rtiamb.getParameterHandle(cashQueueStatsInteractionHandle, "clientsInCashQueues");
        queuesCountCashQueueStatsHandle = rtiamb.getParameterHandle(cashQueueStatsInteractionHandle, "totalCashQueuesCount");
        privilegedCashQueueStatsHandle = rtiamb.getParameterHandle(cashQueueStatsInteractionHandle, "totalCashQueuesPrivilegedCount");

        cashRegisterStatsInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CashRegisterStats");
        boughtProductsCashRegisterHandle = rtiamb.getParameterHandle(cashRegisterStatsInteractionHandle, "totalProductsBought");
        returnedProductsCashRegisterHandle = rtiamb.getParameterHandle(cashRegisterStatsInteractionHandle, "totalProductsReturned");
        usedPrivilegedCashRegisterHandle = rtiamb.getParameterHandle(cashRegisterStatsInteractionHandle, "clientsUsedPrivilegedCashRegister");

        rtiamb.subscribeInteractionClass(cashQueueStatsInteractionHandle);
        rtiamb.subscribeInteractionClass(cashRegisterStatsInteractionHandle);
        rtiamb.subscribeInteractionClass(shopStatsInteractionHandle);
    }

    private void stopSimulation() throws RTIexception {

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[StopSimulationInteraction] Send interaction [TIME: %.1f]", newTimeDouble));

        rtiamb.sendInteraction(stopSimulationInteractionHandle, parameters, generateTag(), time);
    }

    public  void setGuiHandler(GUIController newOne){
        this.guihandler=newOne;
    }



    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main(String[] args)
    {
        String federateName = "AppInterfaceFederate";
        if( args.length != 0 ) {
            federateName = args[0];
        }

        try {
            AppInterfaceFederate appInterfaceFederate = new AppInterfaceFederate();
            EventQueue.invokeLater(() -> GUI.start(appInterfaceFederate));
            appInterfaceFederate.runFederate(federateName);
        }
        catch( Exception rtie ) {
            rtie.printStackTrace();
        }
    }
}
