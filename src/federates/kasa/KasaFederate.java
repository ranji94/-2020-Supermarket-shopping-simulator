package federates.kasa;

import entity.Interfejs;
import entity.Kasa;
import entity.Sklep;
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

public class KasaFederate {
    private static final Logger logger = new Logger("KasaFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private KasaFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    // SUBSCRIBED Interactions
    protected InteractionClassHandle otworzKaseInteractionHandle;
    protected ParameterHandle idKasyHandle;
    protected ParameterHandle uprzywilejowanaHandle;
    protected ParameterHandle czasObslugiHandle;

    protected InteractionClassHandle klientDoKasyInteractionHandle;
    protected ParameterHandle idKlientaDoKasyHandle;
    protected ParameterHandle idKasyDoKasyHandle;
    protected ParameterHandle iloscProduktowDoKasyHandle;

    // PUBLISHED Interactions
    protected InteractionClassHandle koniecZakupowInteractionHandle;
    protected ParameterHandle idKlientaKoniecHandle;
    protected ParameterHandle idKasyKoniecHandle;
    ////////////////////////////////////////////////////

    private boolean sklepIsOpen = true;
    private int sumaWszystkichZakupionychProduktow = 0;

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
        fedamb = new KasaFederateAmbassador( this );
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
                "KasaFederate",
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
        while(sklepIsOpen) {
            advanceTime(randomTime());
            if (Constants.LOG_TIME_ADVANCE) logger.info(String.format("Time Advanced to %.1f", fedamb.federateTime));

            if(fedamb.getExternalEvents().size() > 0) {
                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                    switch (externalEvent.getEventType()) {
                        case OTWORZ_KASE:
                            Object [] otworzKaseData = (Object [])externalEvent.getData();
                            otworzKaseInteractionReceived(otworzKaseData);
                            break;
                        case KLIENT_DO_KASY:
                            Object [] klientDoKasyData = (Object [])externalEvent.getData();
                            klientDoKasyInteractionReceived(klientDoKasyData);
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }


        }
    }

    private void publishAndSubscribe() throws RTIexception
    {
        //PUBLISHED DECLARATIONS
        koniecZakupowInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.KoniecZakupow");
        idKlientaKoniecHandle = rtiamb.getParameterHandle(koniecZakupowInteractionHandle, "idKlient");
        idKasyKoniecHandle = rtiamb.getParameterHandle(koniecZakupowInteractionHandle, "idKasy");

        // SUBSCRIBED DECLARATIONS
        otworzKaseInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OtworzKase");
        idKasyHandle = rtiamb.getParameterHandle(otworzKaseInteractionHandle, "idKasy");
        uprzywilejowanaHandle = rtiamb.getParameterHandle(otworzKaseInteractionHandle, "uprzywilejowana");
        czasObslugiHandle = rtiamb.getParameterHandle(otworzKaseInteractionHandle, "czasObslugi");

        klientDoKasyInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.KlientDoKasy");
        idKlientaDoKasyHandle = rtiamb.getParameterHandle(klientDoKasyInteractionHandle, "idKlient");
        idKasyDoKasyHandle = rtiamb.getParameterHandle(klientDoKasyInteractionHandle, "idKasy");
        iloscProduktowDoKasyHandle = rtiamb.getParameterHandle(klientDoKasyInteractionHandle, "iloscProduktow");

        // PUBLISHED
        rtiamb.publishInteractionClass(koniecZakupowInteractionHandle);

        // SUBSCRIBED
        rtiamb.subscribeInteractionClass(otworzKaseInteractionHandle);
        rtiamb.subscribeInteractionClass(klientDoKasyInteractionHandle);
    }

    private void otworzKaseInteractionReceived(Object[] data) {
        String idKasyIKolejki = (String) data[0];
        boolean uprzywilejowana = (boolean) data[1];
        int czasObslugi = (int) data[2];

        Kasa kasa = new Kasa(idKasyIKolejki, idKasyIKolejki, null, czasObslugi, uprzywilejowana);
        Interfejs.getInstance().getWszystkieKasy().put(idKasyIKolejki, kasa);

        logger.info(String.format("[OtworzKaseInteractionReceived] Otwarto Kase o ID: %s, uprzywilejowana: %s. Otwartych kas: %s",
                idKasyIKolejki,
                uprzywilejowana,
                Interfejs.getInstance().getWszystkieKasy().size()));
    }

    private void klientDoKasyInteractionReceived(Object[] data) {
        Random rand = new Random();
        String idKasy = (String) data[0];
        String idKlient = (String) data[1];
        sumaWszystkichZakupionychProduktow += (int) data[2];

        Kasa kasa = Interfejs.getInstance().getWszystkieKasy().get(idKasy);
        kasa.setIdAktualnyKlient(idKlient);
        int czasObslugiNormalized = 11 - kasa.getCzasObslugi();
        int randomValue = rand.nextInt(czasObslugiNormalized) + 1;

        if(czasObslugiNormalized == randomValue) {
            try {
                koniecZakupowInteraction(idKlient, kasa.getIdKasy());
            } catch (RTIexception e) {
                logger.info("[KoniecZakupowInteraction] RTIException error");
            }
        }

        logger.info(String.format("[KlientDoKasy] Klient %s dołączył do kasy %s. Kupionych w sumie produktów: %s", idKlient, idKasy, sumaWszystkichZakupionychProduktow));
    }

    private void koniecZakupowInteraction(String idKlient, String idKasy) throws RTIexception {
        HLAunicodeString idKlientValue = encoder.createHLAunicodeString(idKlient);
        HLAunicodeString idKasyValue = encoder.createHLAunicodeString(idKasy);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(idKlientaKoniecHandle, idKlientValue.toByteArray());
        parameters.put(idKasyKoniecHandle, idKasyValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[KoniecZakupowInteraction] Send interaction klientId: %s [TIME: %.1f]", idKlient,  newTimeDouble));

        rtiamb.sendInteraction(koniecZakupowInteractionHandle, parameters, generateTag(), time);
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main( String[] args )
    {
        String federateName = "KasaFederate";
        if( args.length != 0 ) {
            federateName = args[0];
        }

        try {
            new KasaFederate().runFederate( federateName );
        }
        catch( Exception rtie ) {
            rtie.printStackTrace();
        }
    }
}
