package federates.kolejka;

import entity.Klient;
import entity.Kolejka;
import entity.Sklep;
import events.ExternalEvent;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import repository.SklepRepository;
import repository.SklepRepositoryImpl;
import utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class KolejkaFederate {
    private static final Logger logger = new Logger("KolejkaFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private KolejkaFederateAmbassador fedamb;
    protected EncoderDecoder encoder;
    SklepRepository sklepRepository = new SklepRepositoryImpl();

    // Interactions Published
    protected InteractionClassHandle otworzKaseInteractionHandle;
    protected ParameterHandle idKolejkiOtworzHandle;
    protected ParameterHandle idKasyOtworzHandle;
    protected ParameterHandle uprzywilejowanaOtworzHandle;

    protected InteractionClassHandle zamknijKaseInteractionHandle;
    protected ParameterHandle idKasyZamknijHandle;

    protected InteractionClassHandle nastepnyKlientInteractionHandle;
    protected ParameterHandle idKlientaNastepnyHandle;
    protected ParameterHandle idKasyNastepnyHandle;

    // Interactions Subscribed
    protected InteractionClassHandle otworzKolejkeInteractionHandle;
    protected ParameterHandle idKolejkiOtworzSubscribedHandle;

    protected InteractionClassHandle zamknijKolejkeInteractionHandle;
    protected ParameterHandle idKolejkiZamknijHandle;

    protected InteractionClassHandle klientDoKolejkiInteractionHandle;
    protected ParameterHandle idKlientaKolejkiHandle;
    protected ParameterHandle idKolejkiKlientHandle;

    /////////////////////////////////////////////////

    private boolean sklepIsOpen = true;

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
        fedamb = new KolejkaFederateAmbassador( this );
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
                "KolejkaFederate",
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
                        case OTWORZ_KOLEJKE:
                            String kolejkaId = (String) externalEvent.getData();
                            otworzKolejkeReceived(kolejkaId);
                            break;
                        case ZAMKNIJ_KOLEJKE:
                            zamknijKolejkeReceived();
                            break;
                        case KLIENT_DO_KOLEJKI:
                            String klientId = (String) externalEvent.getData();
                            klientDoKolejkiReceived(klientId);
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }
        }
    }

    private void otworzKolejkeReceived(String idKolejki) {
        Random rand = new Random();
        Sklep sklep = Sklep.getInstance();

        Kolejka kolejka = new Kolejka();
        kolejka.setIdKolejki(idKolejki);
        kolejka.setIdKasy(idKolejki);
        kolejka.setSredniCzasObslugi(rand.nextInt(10 + 1) + 1);
        kolejka.setListaKlientow(new ArrayList<>());

        Map<String, Kolejka> kolejki = sklep.getWszystkieKolejkiWSklepie();
        kolejki.put(kolejka.getIdKolejki(), kolejka);
        sklep.setWszystkieKolejkiWSklepie(kolejki);

        logger.info(String.format("Otworzono kolejkę o id: %s, suma otwartych kolejek w sklepie: %s", idKolejki, sklep.getWszystkieKolejkiWSklepie().size()));
    }

    private void zamknijKolejkeReceived() {

    }

    private void klientDoKolejkiReceived(String idKlient) {
        Klient klient = sklepRepository.findClientById(idKlient);
        Kolejka najkrotszaKolejka = sklepRepository.findShortestQueue();

        sklepRepository.addClientToQueue(klient, najkrotszaKolejka);

        logger.info(String.format("Klient %s, dołączył do kolejki %s. Suma klientów w kolejce: %s", idKlient, najkrotszaKolejka.toString(), Sklep.getInstance().getWszystkieKolejkiWSklepie().size()));
    }

    private void publishAndSubscribe() throws RTIexception
    {
        // PUBISHED
        otworzKaseInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OtworzKase");
        idKasyOtworzHandle = rtiamb.getParameterHandle(otworzKaseInteractionHandle, "idKasy");
        idKolejkiOtworzHandle = rtiamb.getParameterHandle(otworzKaseInteractionHandle, "idKolejki");

        zamknijKaseInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ZamknijKase");
        idKasyZamknijHandle = rtiamb.getParameterHandle(zamknijKaseInteractionHandle, "idKasy");

        nastepnyKlientInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.NastepnyKlient");
        idKlientaNastepnyHandle = rtiamb.getParameterHandle(nastepnyKlientInteractionHandle, "idKlient");
        idKasyNastepnyHandle = rtiamb.getParameterHandle(nastepnyKlientInteractionHandle, "idKasy");

        // SUBSCRIBED
        otworzKolejkeInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OtworzKolejke");
        idKolejkiOtworzSubscribedHandle = rtiamb.getParameterHandle(otworzKolejkeInteractionHandle, "idKolejki");

        zamknijKolejkeInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ZamknijKolejke");
        idKolejkiZamknijHandle = rtiamb.getParameterHandle(zamknijKolejkeInteractionHandle, "idKolejki");

        klientDoKolejkiInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.KlientDoKolejki");
        idKolejkiKlientHandle = rtiamb.getParameterHandle(klientDoKolejkiInteractionHandle, "idKolejki");
        idKlientaKolejkiHandle = rtiamb.getParameterHandle(klientDoKolejkiInteractionHandle, "idKlient");
        ////////////////////////////////////////

        rtiamb.publishInteractionClass(otworzKaseInteractionHandle);
        rtiamb.publishInteractionClass(zamknijKaseInteractionHandle);
        rtiamb.publishInteractionClass(nastepnyKlientInteractionHandle);

        rtiamb.subscribeInteractionClass(otworzKolejkeInteractionHandle);
        rtiamb.subscribeInteractionClass(zamknijKaseInteractionHandle);
        rtiamb.subscribeInteractionClass(klientDoKolejkiInteractionHandle);
    }

    private void otworzKaseInteraction() throws RTIexception {
        String kasaKolejkaId = UUIDUtils.shortId();
        HLAunicodeString kasaKolejkaIdValue = encoder.createHLAunicodeString(kasaKolejkaId);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(idKasyOtworzHandle, kasaKolejkaIdValue.toByteArray());
        parameters.put(idKolejkiOtworzHandle, kasaKolejkaIdValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[OtworzKaseInteraction] Send interaction, kasaId and kolejkaId: %s [TIME: %.1f]", kasaKolejkaId, newTimeDouble));

        rtiamb.sendInteraction(otworzKaseInteractionHandle, parameters, generateTag(), time);
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main( String[] args )
    {
        String federateName = "KolejkaFederate";
        if( args.length != 0 ) {
            federateName = args[0];
        }

        try {
            new KolejkaFederate().runFederate( federateName );
        }
        catch( Exception rtie ) {
            rtie.printStackTrace();
        }
    }
}
