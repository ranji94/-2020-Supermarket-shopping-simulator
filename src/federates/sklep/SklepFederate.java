package federates.sklep;

import entity.Klient;
import entity.Kolejka;
import entity.Sklep;
import federates.klient.KlientFederate;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;

import events.ExternalEvent;
import repository.SklepRepository;
import repository.SklepRepositoryImpl;
import utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SklepFederate {
    private static final Logger logger = new Logger("SklepFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private SklepFederateAmbassador fedamb;
    protected EncoderDecoder encoder;
    SklepRepository sklepRepository = new SklepRepositoryImpl();

    //Published Interactions
    protected InteractionClassHandle otworzKolejkeInteractionHandle;
    protected ParameterHandle idKolejkiOtworzHandle;

    protected InteractionClassHandle zamknijKolejkeInteractionHandle;
    protected ParameterHandle idKolejkiZamknijHandle;

    protected InteractionClassHandle klientDoKolejkiInteractionHandle;
    protected ParameterHandle idKlientaKolejkiHandle;
    protected ParameterHandle idKolejkiKlientHandle;

    protected InteractionClassHandle koniecZakupowInteractionHandle;
    protected ParameterHandle idKlientaKoniecHandle;
    protected ParameterHandle iloscProduktowHandle;

    //Subscribed Interactions
    protected InteractionClassHandle klientWchodziInteractionHandle;
    protected ParameterHandle idKlientWchodziHandle;

    protected InteractionClassHandle stopSimulationInteractionHandle;

    private boolean stopSimulation = false;
    private boolean kolejkaPoczatkowaIstnieje = false;

    /////////////////////////////////////////////////////////////////////////

    private double randomTime() {
        Random r = new Random();
        return 1 +(4 * r.nextDouble());
    }

    private void run() throws RTIexception {
        while (!stopSimulation) {
            Sklep sklep = Sklep.getInstance();
            logger.info(String.format("[SklepFederate] Wszyscy klienci w sklepie: %s, klienci na zakupach: %s, klienci w kolejkach: %s, liczba kolejek: %s",
                    sklep.getSumaWszystkichKlientow(),
                    sklep.getSumaKlientowZakupy(),
                    sklep.getSumaKlientowKolejka()));
            boolean isClientsInShop = sklep.getWszyscyKlienciWSklepie().size() > 0;

            advanceTime(Constants.TIME_STEP);
            if (Constants.LOG_TIME_ADVANCE) logger.info(String.format("Time Advanced to %.1f", fedamb.federateTime));

            if(fedamb.getExternalEvents().size() > 0) {
                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                    switch (externalEvent.getEventType()) {
                        case KLIENT_WCHODZI:
                            if(Sklep.getInstance().getWszyscyKlienciWSklepie().size() < Constants.MAX_KLIENTOW_W_SKLEPIE) {
                                String klientId = (String) externalEvent.getData();
                                klientWchodziReceived(klientId);
                            }
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }

            if (isClientsInShop) {
                Random rand = new Random();
                int coKtoryKlientKonczyZakupy = Math.round(1 / Constants.PRAWDOPODOBIENSTWO_ZAKONCZENIA_ZAKUPOW);
                int randomValue = rand.nextInt(coKtoryKlientKonczyZakupy) + 1;
                List<String> allClientIds = new ArrayList<>(Sklep.getInstance().getWszyscyKlienciWSklepie().keySet());

                if (coKtoryKlientKonczyZakupy == randomValue) {
                    String wylosowanyKlientId = allClientIds.get(rand.nextInt(allClientIds.size()));
                    logger.info(String.format("Wylosowano klienta o ID: %s", wylosowanyKlientId));

                    if(!kolejkaPoczatkowaIstnieje) {
                        String idKolejki = UUIDUtils.shortId();
                        otworzKolejkeInteraction(idKolejki);
                        kolejkaPoczatkowaIstnieje=true;
                    }

                    klientDoKolejkiInteraction(wylosowanyKlientId);
                }
            }
        }
    }

    private void klientWchodziReceived(String klientId) {
        logger.info(String.format("[KlientWchodziReceivedInteraction]: Client with id: %s, entered to shop.", klientId));
        Random rand = new Random();

        float probability = Constants.PROCENT_KLIENTOW_KUPUJACYCH_5_PRODUKTOW / 100f;
        int coKtoryKlient = Math.round(1 / probability);
        int randomizedClient = rand.nextInt(coKtoryKlient) + 1;

        Klient klient = new Klient(klientId);

        if(coKtoryKlient != randomizedClient) {
            klient.setIloscProduktow(rand.nextInt(Constants.MAX_PRODUKTOW_KLIENTA + 1) + 5);
        }
        else {
            klient.setIloscProduktow(rand.nextInt(5) + 1);
        }

        logger.info(String.format("[KlientWchodziReceived] Utworzono klienta: %s, sklepRepositoryInerface: %s", klient.toString(), sklepRepository));

        sklepRepository.addClientToShop(klient);
    }

    private void publishAndSubscribe() throws RTIexception
    {
        //PUBLISHED
        otworzKolejkeInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OtworzKolejke");
        idKolejkiOtworzHandle = rtiamb.getParameterHandle(otworzKolejkeInteractionHandle, "idKolejki");

        zamknijKolejkeInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ZamknijKolejke");
        idKolejkiZamknijHandle = rtiamb.getParameterHandle(zamknijKolejkeInteractionHandle, "idKolejki");

        klientDoKolejkiInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.KlientDoKolejki");
        idKolejkiKlientHandle = rtiamb.getParameterHandle(klientDoKolejkiInteractionHandle, "idKolejki");
        idKlientaKolejkiHandle = rtiamb.getParameterHandle(klientDoKolejkiInteractionHandle, "idKlient");

        //SUBSCRIBED
        klientWchodziInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.KlientWchodzi");
        idKlientWchodziHandle = rtiamb.getParameterHandle(klientWchodziInteractionHandle, "idKlient");

        //////////////////////////////////////////////////////////////

        rtiamb.subscribeInteractionClass(klientWchodziInteractionHandle);

        rtiamb.publishInteractionClass(otworzKolejkeInteractionHandle);
        rtiamb.publishInteractionClass(zamknijKolejkeInteractionHandle);
        rtiamb.publishInteractionClass(klientDoKolejkiInteractionHandle);
    }

    private void klientDoKolejkiInteraction(String klientId) throws RTIexception {
        HLAunicodeString klientIdValue = encoder.createHLAunicodeString(klientId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(idKlientaKolejkiHandle, klientIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[KlientDoKolejkiInteraction] Send interaction, klientId: %s, [TIME: %.1f]", klientId, newTimeDouble));

        rtiamb.sendInteraction(klientDoKolejkiInteractionHandle, parameters, generateTag(), time);
    }

    private void otworzKolejkeInteraction(String kolejkaId) throws RTIexception {
        HLAunicodeString kolejkaIdValue = encoder.createHLAunicodeString(kolejkaId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(idKolejkiOtworzHandle, kolejkaIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[OtworzKolejkeInteraction] Send interaction, kolejkaId: %s [TIME: %.1f]", kolejkaId, newTimeDouble));

        rtiamb.sendInteraction(otworzKolejkeInteractionHandle, parameters, generateTag(), time);
    }

    private void zamknijKolejkeInteraction(String kolejkaId) throws RTIexception {
        HLAunicodeString kolejkaIdValue = encoder.createHLAunicodeString(kolejkaId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(idKolejkiZamknijHandle, kolejkaIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[ZamknijKolejkeInteraction] Send interaction, kolejkaId: %s [TIME: %.1f]", kolejkaId, newTimeDouble));

        rtiamb.sendInteraction(zamknijKolejkeInteractionHandle, parameters, generateTag(), time);
    }

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
                "SklepFederateType",
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

    private void endSimulation() {
        stopSimulation = true;
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main( String[] args )
    {
        String sklepFederateName = "SklepFederate";
        if( args.length != 0 ) {
            sklepFederateName = args[0];
        }

        try {
            SklepFederate federate = new SklepFederate();
            federate.runFederate( sklepFederateName );
        }
        catch( Exception rtie ) {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
