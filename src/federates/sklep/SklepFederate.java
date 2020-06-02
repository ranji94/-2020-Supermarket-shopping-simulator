package federates.sklep;

import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;

import events.ExternalEvent;
import utils.Constants;
import utils.EncoderDecoder;
import utils.Logger;
import utils.TimeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class SklepFederate {
    private static final Logger logger = new Logger("SklepFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private SklepFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    //Interactions
    protected InteractionClassHandle otworzKolejkeInteractionHandle;
    protected ParameterHandle idKolejkiOtworzHandle;

    protected InteractionClassHandle zamknijKolejkeInteractionHandle;
    protected ParameterHandle idKolejkiZamknijHandle;

    protected InteractionClassHandle klientDoKolejkiInteractionHandle;
    protected ParameterHandle idKlientaHandle;
    protected ParameterHandle idKolejkiKlientHandle;

    protected InteractionClassHandle koniecZakupowInteractionHandle;
    protected ParameterHandle idKlientaKoniecHandle;
    protected ParameterHandle iloscProduktowHandle;

    protected InteractionClassHandle stopSimulationInteractionHandle;

    private boolean stopSimulation = false;
    private int initialClientsToCreate = Constants.POCZATKOWA_LICZBA_KLIENTOW;


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

    public void runFederate( String federateName ) throws Exception
    {
        // create rti
        logger.info( "Creating RTIambassador" );
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoder = new EncoderDecoder();

        // connect
        logger.info( "Connecting..." );
        fedamb = new SklepFederateAmbassador( this );
        rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );

        // create federation
        logger.info( "Creating Federation..." );
        try
        {
            URL[] modules = new URL[]{
                    (new File("HLAstandardMIM.xml")).toURI().toURL(),
                    (new File("fom.xml")).toURI().toURL()
            };

            rtiamb.createFederationExecution( "SklepFederation", modules );
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
                "SklepFederateType",
                "SklepFederation",
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
//        publishAndSubscribe();
        logger.info( "Published and Subscribed" );

//        run();

        // resign from the federation
        rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
        logger.info( "Resigned from Federation" );

        //try and destroy the federation
        try
        {
            rtiamb.destroyFederationExecution( "SkiStationFederation" );
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

    private double randomTime() {
        Random r = new Random();
        return 1 +(4 * r.nextDouble());
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

    private byte[] generateTag()
    {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    /////////////////////////////////////////////////////////////////////////

//    private void run() throws RTIexception {
//        while (initialClientsToCreate > 0) {
//            advanceTime(randomTime());
//            if (Constants.LOG_TIME_ADVANCE) logger.info(String.format("Time Advanced to %.1f", fedamb.federateTime));
//
//            generateNewSkier();
//            numberOfSkiersToCreate--;
//
//            if (Math.round(fedamb.federateTime) % Constants.CONDITION_GENERATION_PERIOD == 0) {
//                generateNewSkiStationCondition();
//            }
//        }
//
//        closeSkiStationInteractionSend();
//
//        while (!stopSimulation) {
//            advanceTime(Constants.TIME_STEP);
//            if (Constants.LOG_TIME_ADVANCE) logger.info(String.format("Time Advanced to %.1f", fedamb.federateTime));
//
//            if(fedamb.getExternalEvents().size() > 0) {
//                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
//                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
//                    switch (externalEvent.getEventType()) {
//                        case STOP_SIMULATION:
//                            endSimulation();
//                            break;
//                    }
//                }
//                fedamb.getExternalEvents().clear();
//            }
//
//            if (Math.round(fedamb.federateTime) % Constants.CONDITION_GENERATION_PERIOD == 0) {
//                generateNewSkiStationCondition();
//            }
//        }
//    }
}
