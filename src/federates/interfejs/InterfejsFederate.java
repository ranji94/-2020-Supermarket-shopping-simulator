package federates.interfejs;

import entity.Interfejs;
import events.ExternalEvent;
import gui.GUIController;
import gui.GUIHandler;
import gui.GUI;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import utils.*;

import javax.lang.model.type.ArrayType;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class InterfejsFederate {
    private static final Logger logger = new Logger("InterfejsFederate");

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private InterfejsFederateAmbassador fedamb;
    protected EncoderDecoder encoder;

    protected InteractionClassHandle zamknijDrzwiInteractionHandle;

    // SUBSCRIBED interactions
    protected InteractionClassHandle statystykiSklepuInteractionHandle;
    protected ParameterHandle wszyscyKlienciSklepHandle;
    protected ParameterHandle klienciNaZakupachSklepHandle;
    protected ParameterHandle klienciWKolejkachSklepHandle;
    protected ParameterHandle sumaKlientowPoZakupachHandle;

    protected InteractionClassHandle statystykiKolejkiInteractionHandle;
    protected ParameterHandle kolejkiIKlienciKolejkiHandle;
    protected ParameterHandle iloscKolejekKolejkiHandle;
    protected ParameterHandle iloscUprzywilejowanychKolejkiHandle;

    protected InteractionClassHandle statystykiKasaInteractionHandle;
    protected ParameterHandle zakupionychTowarowKasaHandle;
    protected ParameterHandle zwrotyTowarowKasaHandle;
    protected ParameterHandle skorzystaloZUprzywilejowanej;

    protected InteractionClassHandle stopSimulationInteractionHandle;

    //////////////////////////////////////////////////

    private GUIHandler guihandler;

    private boolean sklepIsOpen = true;

    private int wszyscyWSklepie = 0;
    private int sumaWKolejkach = 0;
    private int sumaNaZakupach = 0;
    private int sumaWszystkichObsluzonych = 0;
    private String klienciWKolejkach = "";
    private int sumaKolejek = 0;
    private int kolejekUprzywilejowanych = 0;
    private int zakupionychTowarow = 0;
    private int zwroconychTowarow = 0;
    private int skorzystaloZKasyUprzywilejowanej = 0;
    private int maxDlugoscKas=0;
    private boolean zamknijDrzwi=false;

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
        fedamb = new InterfejsFederateAmbassador( this );
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
                "InterfejsFederate",
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

            if(zamknijDrzwi)zamknijDrzwiInteractionSend();

            if (fedamb.getExternalEvents().size() > 0) {
                fedamb.getExternalEvents().sort(new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.getExternalEvents()) {
                    switch (externalEvent.getEventType()) {
                        case STATYSTYKI_SKLEP:
                            Object[] dataSklep = (Object[]) externalEvent.getData();
                            updateSklepStats(dataSklep);
                            break;
                        case STATYSTYKI_KASA:
                            Object[] dataKasa = (Object[]) externalEvent.getData();
                            updateKasaStats(dataKasa);
                            break;
                        case STATYSTYKI_KOLEJKA:
                            Object[] dataKolejka = (Object[]) externalEvent.getData();
                            updateKolejkaStats(dataKolejka);
                            break;
                        case STOP_SIMULATION:
                            sklepIsOpen=false;
                            break;
                    }
                }
                fedamb.getExternalEvents().clear();
            }

            logger.info(String.format("STATYSTYKI SKLEPU: \n" +
                    "Wszyscy klienci w sklepie: %s \n" +
                    "Klienci robiący zakupy: %s \n" +
                    "Klienci w kolejkach: %s \n" +
                    "Wszyscy obsłużeni klienci: %s \n" +
                    "Wizualizacja kolejek: %s \n" +
                    "Suma kolejek: %s \n" +
                    "Kolejek do kas uprzywilejowanych: %s \n" +
                    "Zakupionych towarów: %s \n" +
                    "Zwróconych towarów: %s \n" +
                    "Suma klientów którzy skorzystali z kasy uprzywilejowanej: %s",
                    wszyscyWSklepie,
                    sumaNaZakupach,
                    sumaWKolejkach,
                    sumaWszystkichObsluzonych,
                    klienciWKolejkach,
                    sumaKolejek,
                    kolejekUprzywilejowanych,
                    zakupionychTowarow,
                    zwroconychTowarow,
                    skorzystaloZKasyUprzywilejowanej));
        }

    }

    private void updateSklepStats(Object [] data) {
        int wszyscyKlienci = (int) data[0];
        int naZakupach = (int) data[1];
        int wKolejkach = (int) data[2];
        int poZakupach = (int) data[3];

        wszyscyWSklepie = wszyscyKlienci;
        sumaNaZakupach = naZakupach;
        sumaWKolejkach = wKolejkach;
        sumaWszystkichObsluzonych = poZakupach;

        guihandler.addWszyscyWSklepie(wszyscyWSklepie);
        guihandler.addWszyscyNaZakupach(sumaNaZakupach);
        guihandler.addWszyscyWKolejce(sumaWKolejkach);
        guihandler.addWszyscyObsluzeni(sumaWszystkichObsluzonych);
    }

    private void updateKolejkaStats(Object [] data) {
        String wszyscyWKolejkach = (String) data[0];
        int sumaKolejek = (int) data[1];
        int sumaKolejekUprzywilejowanych = (int) data[2];

        klienciWKolejkach = wszyscyWKolejkach;
        this.sumaKolejek = sumaKolejek;
        kolejekUprzywilejowanych = sumaKolejekUprzywilejowanych;

        if(maxDlugoscKas<sumaKolejek){
            maxDlugoscKas=sumaKolejek;
            guihandler.addMaxDlugoscKolejek(maxDlugoscKas);
        }

        guihandler.addLiczbeKolejek(sumaKolejek);
        guihandler.obrazowanieKolejek(wszyscyWKolejkach);
        guihandler.addKlienciUprzywilejowani(kolejekUprzywilejowanych);
    }

    private void updateKasaStats(Object [] data) {
        int zakupionychTowarow = (int) data[0];
        int zwroconychTowarow = (int) data[1];
        int skorzystaloZUprzywilejowanej = (int) data[2];

        this.zakupionychTowarow = zakupionychTowarow;
        this.zwroconychTowarow = zwroconychTowarow;
        skorzystaloZKasyUprzywilejowanej = skorzystaloZUprzywilejowanej;

        guihandler.addZakupionychTowarow(zakupionychTowarow);
        guihandler.addZwroconeTowary(zwroconychTowarow);
        guihandler.addKlienciUprzywilejowani(skorzystaloZKasyUprzywilejowanej);
    }

    private void zamknijDrzwiInteractionSend() throws RTIexception{
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(0);

        double newTimeDouble = fedamb.federateTime + fedamb.federateLookahead;
        LogicalTime time = TimeUtils.convertTime(newTimeDouble);

        logger.info(String.format("[ZamknijDrzwiInteraction] Send interaction [TIME: %.1f]", newTimeDouble));

        zamknijDrzwi=false;
        rtiamb.sendInteraction(zamknijDrzwiInteractionHandle,parameters,generateTag(),time);
    }

    private void publishAndSubscribe() throws RTIexception
    {
        zamknijDrzwiInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.ZamknijDrzwi");


        statystykiSklepuInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.StatystykiSklep");
        wszyscyKlienciSklepHandle = rtiamb.getParameterHandle(statystykiSklepuInteractionHandle, "wszyscyKlienci");
        klienciNaZakupachSklepHandle = rtiamb.getParameterHandle(statystykiSklepuInteractionHandle, "naZakupach");
        klienciWKolejkachSklepHandle = rtiamb.getParameterHandle(statystykiSklepuInteractionHandle, "klienciWKolejkach");
        sumaKlientowPoZakupachHandle = rtiamb.getParameterHandle(statystykiSklepuInteractionHandle, "sumaKlientowPoZakupach");

        statystykiKolejkiInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.StatystykiKolejka");
        kolejkiIKlienciKolejkiHandle = rtiamb.getParameterHandle(statystykiKolejkiInteractionHandle, "kolejkiIKlienci");
        iloscKolejekKolejkiHandle = rtiamb.getParameterHandle(statystykiKolejkiInteractionHandle, "iloscKolejek");
        iloscUprzywilejowanychKolejkiHandle = rtiamb.getParameterHandle(statystykiKolejkiInteractionHandle, "iloscKolejekUprzywilejowanych");

        statystykiKasaInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.StatystykiKasa");
        zakupionychTowarowKasaHandle = rtiamb.getParameterHandle(statystykiKasaInteractionHandle, "iloscZakupionychTowarow");
        zwrotyTowarowKasaHandle = rtiamb.getParameterHandle(statystykiKasaInteractionHandle, "iloscZwrotow");
        skorzystaloZUprzywilejowanej = rtiamb.getParameterHandle(statystykiKasaInteractionHandle, "ileSkorzystaloZUprzywilejowanej");

        stopSimulationInteractionHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.StopSimulation");

        rtiamb.publishInteractionClass(zamknijDrzwiInteractionHandle);

        rtiamb.subscribeInteractionClass(statystykiKolejkiInteractionHandle);
        rtiamb.subscribeInteractionClass(statystykiKasaInteractionHandle);
        rtiamb.subscribeInteractionClass(statystykiSklepuInteractionHandle);
        rtiamb.subscribeInteractionClass(stopSimulationInteractionHandle);
    }


    public  void setGuiHandler(GUIController newOne){
        this.guihandler=newOne;
    }

    public void zamknijDrzwiSklepu() throws RTIexception {
        zamknijDrzwi=true;
    }



    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main(String[] args)
    {
        String federateName = "InterfejsFederate";
        if( args.length != 0 ) {
            federateName = args[0];
        }

        try {
            InterfejsFederate interfejsFederate = new InterfejsFederate();
            EventQueue.invokeLater(() -> GUI.start(interfejsFederate));
            interfejsFederate.runFederate(federateName);
        }
        catch( Exception rtie ) {
            rtie.printStackTrace();
        }
    }
}
