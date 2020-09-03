package federates.client;

import events.ExternalEvent;
import hla.rti1516e.*;
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

public class ClientFederate {
    private static final Logger logger = new Logger("ClientFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private ClientFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    // Interactions PUBLISHED
    protected InteractionClassHandle clientEnteredShopInteractionHandle;
    protected ParameterHandle clientIdEnteredHandle;

    protected InteractionClassHandle stopSimulationInteractionHandle;

    // Interactions SUBSCRIBED
    protected InteractionClassHandle shoppingFinishedInteractionHandle;
    protected ParameterHandle clientIdFinishedHandle;

    private int clientsEnteredShop = 0;
    private boolean isShopOpen = true;

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
        fedamb = new ClientFederateAmbassador( this );
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
                "ClientFederate",
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
        return 1 +(Constants.CLIENTS_INFLOW_RATIO * r.nextDouble());
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

            if(Constants.MAX_CLIENTS_IN_SHOP > clientsEnteredShop) {
                clientsEnteredShop++;
                clientEnteredShopInteraction();
            }
            else {
                logger.info("Shop is full of clients. Waiting until any client leave finish shopping and leave the Shop.");
            }

            if (fedamb.getExternalEvents().size() > 0) {
                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                    switch (externalEvent.getEventType()) {
                        case SHOPPING_FINISHED:
                            String clientId = (String) externalEvent.getData();
                            shoppingFinishedInteractionReceived(clientId);
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }
        }

        stopSimulation();
    }

    private void shoppingFinishedInteractionReceived(String clientId) {
        clientsEnteredShop--;
        logger.info(String.format("[shoppingFinishedInteractionReceived] Client with id: %s leaved shop and finished shopping. Clients inside shop left: %s",
                clientId,
                clientsEnteredShop));
    }

    private void publishAndSubscribe() throws RTIexception
    {
        // PUBLISHED
        clientEnteredShopInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ClientEnteredShop");
        clientIdEnteredHandle = rtiamb.getParameterHandle(clientEnteredShopInteractionHandle, "clientId");

        // SUBSCRIBED
        shoppingFinishedInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ShoppingFinished");
        clientIdFinishedHandle = rtiamb.getParameterHandle(shoppingFinishedInteractionHandle, "clientId");

        rtiamb.subscribeInteractionClass(shoppingFinishedInteractionHandle);
        /////////////////////////////////////

        rtiamb.publishInteractionClass(clientEnteredShopInteractionHandle);
    }

    private void clientEnteredShopInteraction() throws RTIexception {
        String clientId = UUIDUtils.shortId();
        HLAunicodeString clientIdValue = encoder.createHLAunicodeString(clientId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(clientIdEnteredHandle, clientIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime( newTimeDouble );

        logger.info(String.format("[clientEnteredShopInteraction] Send interaction, clientId: %s", clientId,  newTimeDouble));

        rtiamb.sendInteraction(clientEnteredShopInteractionHandle, parameters, generateTag(), time);
    }

    private void stopSimulation() throws RTIexception {

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[StopSimulationInteraction] Send interaction [TIME: %.1f]", newTimeDouble));

        rtiamb.sendInteraction(stopSimulationInteractionHandle, parameters, generateTag(), time);
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main( String[] args )
    {
        String federateName = "ClientFederate";
        if( args.length != 0 ) {
            federateName = args[0];
        }

        try {
            new ClientFederate().runFederate( federateName );
        }
        catch( Exception rtie ) {
            rtie.printStackTrace();
        }
    }
}
