package federates.kolejka;

import entity.Interfejs;
import entity.Klient;
import entity.Kolejka;
import entity.Sklep;
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

public class KolejkaFederate {
    private static final Logger logger = new Logger("KolejkaFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private KolejkaFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    // Interactions Published
    protected InteractionClassHandle otworzKaseInteractionHandle;
    protected ParameterHandle idKasyOtworzHandle;
    protected ParameterHandle uprzywilejowanaOtworzHandle;
    protected ParameterHandle czasObslugiHandle;

    protected InteractionClassHandle zamknijKaseInteractionHandle;
    protected ParameterHandle idKasyZamknijHandle;

    protected InteractionClassHandle klientDoKasyInteractionHandle;
    protected ParameterHandle idKlientaDoKasyHandle;
    protected ParameterHandle idKasyDoKasyHandle;
    protected ParameterHandle iloscProduktowDoKasyHandle;

    // Interactions Subscribed
    protected InteractionClassHandle otworzKolejkeInteractionHandle;
    protected ParameterHandle idKolejkiOtworzSubscribedHandle;

    protected InteractionClassHandle zamknijKolejkeInteractionHandle;
    protected ParameterHandle idKolejkiZamknijHandle;

    protected InteractionClassHandle klientDoKolejkiInteractionHandle;
    protected ParameterHandle idKlientaKolejkiHandle;
    protected ParameterHandle iloscProduktowHandle;

    protected InteractionClassHandle koniecZakupowInteractionHandle;
    protected ParameterHandle idKlientaKoniecHandle;
    protected ParameterHandle idKasyKoniecHandle;
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
                            String otworzKolejkeKolejkaId = (String) externalEvent.getData();
                            otworzKolejkeReceived(otworzKolejkeKolejkaId);
                            break;
                        case ZAMKNIJ_KOLEJKE:
                            String zamknijKolejkeKolejkaId = (String) externalEvent.getData();
                            zamknijKolejkeReceived(zamknijKolejkeKolejkaId);
                            break;
                        case KLIENT_DO_KOLEJKI:
                            Object [] klientDoKolejkiData = (Object [])externalEvent.getData();
                            klientDoKolejkiReceived(klientDoKolejkiData);
                            break;
                        case KONIEC_ZAKUPOW:
                            Object [] koniecZakupowData = (Object [])externalEvent.getData();
                            koniecZakupowInteractionReceived(koniecZakupowData);
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }

            //List<String> wszystkieKolejki = new ArrayList<>(Interfejs.getInstance().getWszystkieKolejki().keySet());
            //if(wszystkieKolejki.size() > 0) {
                //Random rand = new Random();
                ////////// TU ZAKONCZYLEM, KOLEJKA MUSI ZOSTAC WYLOSOWANA
                Kolejka wylosowanaKolejka = Interfejs.getInstance().getWszystkieKolejki().get(Interfejs.getInstance().getWszystkieKolejki().size());
            if(wylosowanaKolejka != null) {
                if (wylosowanaKolejka.getDlugoscKolejki() > 0) {
                    wylosowanaKolejka.getListaKlientow().peek().setPrzyKasie(true);
                    Klient klient = wylosowanaKolejka.getListaKlientow().peek();

                    klientDoKasyInteraction(wylosowanaKolejka.getIdKasy(), klient.getIdKlient(), klient.getIloscProduktow());
                }
            }
        }
    }

    private void koniecZakupowInteractionReceived(Object [] data) {
        String idKlient = (String) data[0];
        String idKasa = (String) data[1];

        Interfejs.getInstance().getWszystkieKolejki().get(idKasa).getListaKlientow().remove();
        logger.info(String.format("[KoniecZakupow] Usunięto klienta %s z kolejki %s", idKlient, idKasa));
    }

    private void otworzKolejkeReceived(String idKolejki) {
        Random rand = new Random();

        Kolejka kolejka = new Kolejka();
        kolejka.setIdKolejki(idKolejki);
        kolejka.setIdKasy(idKolejki);
        kolejka.setSredniCzasObslugi(rand.nextInt(10 + 1) + 1);
        kolejka.setListaKlientow(new LinkedList<>());

        Interfejs.getInstance().getWszystkieKolejki().put(kolejka.getIdKolejki(), kolejka);

        try {
            otworzKaseInteraction(idKolejki, false, kolejka.getSredniCzasObslugi());
        } catch (RTIexception e) {
            logger.info("[OtworzKase] RTIException, nie można otworzyć kasy");
        }

        logger.info(String.format("Otworzono kolejkę o id: %s, suma otwartych kolejek w sklepie: %s", idKolejki, Interfejs.getInstance().getWszystkieKolejki().size()));
    }

    private void zamknijKolejkeReceived(String idKolejki) {

    }

    private void klientDoKolejkiReceived(Object[] data) {
        String idKlienta = (String) data[0];
        int iloscProduktow = (int) data[1];
        Kolejka kolejka = Interfejs.getInstance().getNajkrotszaKolejka();
        Klient klient = new Klient(idKlienta, iloscProduktow, false);

        if(kolejka.getDlugoscKolejki() >= Constants.MAX_DLUGOSC_KOLEJKI) {
            String idNowejKolejki = UUIDUtils.shortId();
            otworzKolejkeReceived(idNowejKolejki);
            kolejka = Interfejs.getInstance().getNajkrotszaKolejka();
        }

        kolejka.getListaKlientow().add(klient);
        Interfejs.getInstance().getWszystkieKolejki().put(kolejka.getIdKolejki(), kolejka);

        logger.info(String.format("[KlientDoKolejkiReceived] Klient %s dołączył do kolejki %s. Suma klientów w kolejce: %s",
                klient.getIdKlient(),
                kolejka.getIdKolejki(),
                kolejka.getDlugoscKolejki()));

        List<Integer> dlugosciWszystkichKolejek = new ArrayList<>();
        for(Map.Entry<String, Kolejka> q : Interfejs.getInstance().getWszystkieKolejki().entrySet()) {
            dlugosciWszystkichKolejek.add(q.getValue().getDlugoscKolejki());
        }

        logger.info(String.format("[KlientDoKolejkiReceived] Lista kolejek: %s", dlugosciWszystkichKolejek));
    }

    private void publishAndSubscribe() throws RTIexception
    {
        // PUBLISHED
        otworzKaseInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OtworzKase");
        idKasyOtworzHandle = rtiamb.getParameterHandle(otworzKaseInteractionHandle, "idKasy");
        uprzywilejowanaOtworzHandle = rtiamb.getParameterHandle(otworzKaseInteractionHandle, "uprzywilejowana");
        czasObslugiHandle = rtiamb.getParameterHandle(otworzKaseInteractionHandle, "czasObslugi");

        zamknijKaseInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ZamknijKase");
        idKasyZamknijHandle = rtiamb.getParameterHandle(zamknijKaseInteractionHandle, "idKasy");

        klientDoKasyInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.KlientDoKasy");
        idKlientaDoKasyHandle = rtiamb.getParameterHandle(klientDoKasyInteractionHandle, "idKlient");
        idKasyDoKasyHandle = rtiamb.getParameterHandle(klientDoKasyInteractionHandle, "idKasy");
        iloscProduktowDoKasyHandle = rtiamb.getParameterHandle(klientDoKasyInteractionHandle, "iloscProduktow");

        // SUBSCRIBED
        otworzKolejkeInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.OtworzKolejke");
        idKolejkiOtworzSubscribedHandle = rtiamb.getParameterHandle(otworzKolejkeInteractionHandle, "idKolejki");

        zamknijKolejkeInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ZamknijKolejke");
        idKolejkiZamknijHandle = rtiamb.getParameterHandle(zamknijKolejkeInteractionHandle, "idKolejki");

        koniecZakupowInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.KoniecZakupow");
        idKlientaKoniecHandle = rtiamb.getParameterHandle(koniecZakupowInteractionHandle, "idKlient");
        idKasyKoniecHandle = rtiamb.getParameterHandle(koniecZakupowInteractionHandle, "idKasy");

        klientDoKolejkiInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.KlientDoKolejki");
        iloscProduktowHandle = rtiamb.getParameterHandle(klientDoKolejkiInteractionHandle, "iloscProduktow");
        idKlientaKolejkiHandle = rtiamb.getParameterHandle(klientDoKolejkiInteractionHandle, "idKlient");
        ////////////////////////////////////////

        rtiamb.publishInteractionClass(otworzKaseInteractionHandle);
        rtiamb.publishInteractionClass(zamknijKaseInteractionHandle);
        rtiamb.publishInteractionClass(klientDoKasyInteractionHandle);

        rtiamb.subscribeInteractionClass(otworzKolejkeInteractionHandle);
        rtiamb.subscribeInteractionClass(koniecZakupowInteractionHandle);
        rtiamb.subscribeInteractionClass(zamknijKaseInteractionHandle);
        rtiamb.subscribeInteractionClass(klientDoKolejkiInteractionHandle);
    }

    private void otworzKaseInteraction(String kasaKolejkaId, boolean uprzywilejowana, int czasObslugi) throws RTIexception {
        HLAunicodeString kasaKolejkaIdValue = encoder.createHLAunicodeString(kasaKolejkaId);
        HLAboolean uprzywilejowanaValue = encoder.createHLAboolean(uprzywilejowana);
        HLAinteger32BE czasObslugiValue = encoder.createHLAinteger32BE(czasObslugi);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(idKasyOtworzHandle, kasaKolejkaIdValue.toByteArray());
        parameters.put(uprzywilejowanaOtworzHandle, uprzywilejowanaValue.toByteArray());
        parameters.put(czasObslugiHandle, czasObslugiValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[OtworzKaseInteraction] Send interaction, kasaId and kolejkaId: %s [TIME: %.1f]", kasaKolejkaId, newTimeDouble));

        rtiamb.sendInteraction(otworzKaseInteractionHandle, parameters, generateTag(), time);
    }

    private void klientDoKasyInteraction(String idKasy, String idKlient, int iloscProduktow) throws RTIexception {
        HLAunicodeString idKasyValue = encoder.createHLAunicodeString(idKasy);
        HLAunicodeString idKlientValue = encoder.createHLAunicodeString(idKlient);
        HLAinteger32BE iloscProduktowValue = encoder.createHLAinteger32BE(iloscProduktow);

        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);
        parameters.put(idKasyDoKasyHandle, idKasyValue.toByteArray());
        parameters.put(idKlientaDoKasyHandle, idKlientValue.toByteArray());
        parameters.put(iloscProduktowDoKasyHandle, iloscProduktowValue.toByteArray());

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[KlientDoKasyInteraction] Send interaction, kasaId: %s and klientId: %s [TIME: %.1f]", idKasy,  idKlient,  newTimeDouble));

        rtiamb.sendInteraction(klientDoKasyInteractionHandle, parameters, generateTag(), time);
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
