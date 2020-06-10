package federates.klient;

import events.ExternalEvent;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import utils.EncoderDecoder;
import utils.Logger;
import utils.TimeUtils;

import java.util.ArrayList;

public class KlientFederateAmbassador extends NullFederateAmbassador {
    private static final Logger logger = new Logger("KlientFederateAmbassador");
    private EncoderDecoder encoder;
    private KlientFederate federate;

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

    public KlientFederateAmbassador(KlientFederate federate) throws RTIexception {
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
        if( label.equals(KlientFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failedToSyncSet) throws FederateInternalError {
        logger.info( "Federation Synchronized: " + label );
        if( label.equals(KlientFederate.READY_TO_RUN) )
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
        if (interactionClass.equals(federate.stopSimulationInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction StopSimulation [TIME:%.1f]", receiveTime));

            externalEvents.add(new ExternalEvent(null, ExternalEvent.EventType.STOP_SIMULATION, receiveTime));
        }

        if (interactionClass.equals(federate.koniecZakupowInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction KoniecZakupow [TIME:%.1f]", receiveTime));
            String idKlient = encoder.toString(theParameters.get(federate.idKlientKoniecHandle));

            externalEvents.add(new ExternalEvent(idKlient, ExternalEvent.EventType.KONIEC_ZAKUPOW, receiveTime));
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
