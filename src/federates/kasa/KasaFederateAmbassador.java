package federates.kasa;

import events.ExternalEvent;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import utils.EncoderDecoder;
import utils.Logger;
import utils.TimeUtils;

import java.util.ArrayList;

public class KasaFederateAmbassador extends NullFederateAmbassador {
    private static final Logger logger = new Logger("KasaFederateAmbassador");
    private EncoderDecoder encoder;
    private KasaFederate federate;

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

    public KasaFederateAmbassador(KasaFederate federate) throws RTIexception {
        this.federate = federate;
        this.encoder = new EncoderDecoder();
    }

    @Override
    public void synchronizationPointRegistrationSucceeded(String label) throws FederateInternalError {
        logger.info( "Successfully registered sync point: " + label );
    }

    @Override
    public void synchronizationPointRegistrationFailed(String label, SynchronizationPointFailureReason reason) throws FederateInternalError {
        logger.info( "Failed to register sync point: " + label + ", reason="+reason );
    }

    @Override
    public void announceSynchronizationPoint(String label, byte[] tag) throws FederateInternalError {
        logger.info( "Synchronization point announced: " + label );
        if( label.equals(KasaFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failedToSyncSet) throws FederateInternalError {
        logger.info( "Federation Synchronized: " + label );
        if( label.equals(KasaFederate.READY_TO_RUN) )
            this.isReadyToRun = true;
    }

    @Override
    public void receiveInteraction( InteractionClassHandle interactionClass,
                                    ParameterHandleValueMap theParameters,
                                    byte[] tag,
                                    OrderType sentOrdering,
                                    TransportationTypeHandle theTransport,
                                    SupplementalReceiveInfo receiveInfo )
            throws FederateInternalError
    {
        this.receiveInteraction( interactionClass,
                theParameters,
                tag,
                sentOrdering,
                theTransport,
                null,
                sentOrdering,
                receiveInfo );
    }

    @Override
    public void receiveInteraction( InteractionClassHandle interactionClass,
                                    ParameterHandleValueMap theParameters,
                                    byte[] tag,
                                    OrderType sentOrdering,
                                    TransportationTypeHandle theTransport,
                                    LogicalTime time,
                                    OrderType receivedOrdering,
                                    SupplementalReceiveInfo receiveInfo )
            throws FederateInternalError
    {
        if (interactionClass.equals(federate.otworzKaseInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction OtworzKase [TIME:%.1f]", receiveTime));

            String idKasy = encoder.toString(theParameters.get(federate.idKasyHandle));
            boolean uprzywilejowana = encoder.toBoolean(theParameters.get(federate.uprzywilejowanaHandle));
            int czasObslugi = encoder.toInteger32(theParameters.get(federate.czasObslugiHandle));
            Object [] data = { idKasy, uprzywilejowana, czasObslugi };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.OTWORZ_KASE, receiveTime));
        }

        if (interactionClass.equals(federate.zamknijKaseInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction ZamknijKase [TIME:%.1f]", receiveTime));
            String idKasy = encoder.toString(theParameters.get(federate.idKasyZamknijHandle));

            externalEvents.add(new ExternalEvent(idKasy, ExternalEvent.EventType.ZAMKNIJ_KASE, receiveTime));
        }

        if (interactionClass.equals(federate.klientDoKasyInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction KlientDoKasy [TIME:%.1f]", receiveTime));

            String idKasy = encoder.toString(theParameters.get(federate.idKasyDoKasyHandle));
            String idKlient = encoder.toString(theParameters.get(federate.idKlientaDoKasyHandle));
            int iloscProduktow = encoder.toInteger32(theParameters.get(federate.iloscProduktowDoKasyHandle));
            Object [] data = { idKasy, idKlient, iloscProduktow };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.KLIENT_DO_KASY, receiveTime));
        }
    }

    @Override
    public void timeRegulationEnabled(LogicalTime time) throws FederateInternalError {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled(LogicalTime time) throws FederateInternalError {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant(LogicalTime time) throws FederateInternalError {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isAdvancing = false;
    }

    public ArrayList<ExternalEvent> getExternalEvents() {
        return externalEvents;
    }
}
