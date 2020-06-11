package federates.interfejs;

import events.ExternalEvent;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import utils.EncoderDecoder;
import utils.Logger;
import utils.TimeUtils;

import java.util.ArrayList;

public class InterfejsFederateAmbassador extends NullFederateAmbassador {
    private static final Logger logger = new Logger("InterfejsFederateAmbassador");
    private EncoderDecoder encoder;
    private InterfejsFederate federate;

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

    public InterfejsFederateAmbassador(InterfejsFederate federate) throws RTIexception {
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
        if( label.equals(InterfejsFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failedToSyncSet) throws FederateInternalError {
        logger.info( "Federation Synchronized: " + label );
        if( label.equals(InterfejsFederate.READY_TO_RUN) )
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

        if (interactionClass.equals(federate.statystykiSklepuInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction StatystykiSklep [TIME:%.1f]", receiveTime));
            int wszyscyKlienci = encoder.toInteger32(theParameters.get(federate.wszyscyKlienciSklepHandle));
            int naZakupach = encoder.toInteger32(theParameters.get(federate.klienciNaZakupachSklepHandle));
            int wKolejkach = encoder.toInteger32(theParameters.get(federate.klienciWKolejkachSklepHandle));
            int poZakupach = encoder.toInteger32(theParameters.get(federate.sumaKlientowPoZakupachHandle));

            Object [] data = { wszyscyKlienci, naZakupach, wKolejkach, poZakupach };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.STATYSTYKI_SKLEP, receiveTime));
        }

        if (interactionClass.equals(federate.statystykiKolejkiInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction StatystykiKolejka [TIME:%.1f]", receiveTime));
            String wszyscyWKolejkach = encoder.toString(theParameters.get(federate.kolejkiIKlienciKolejkiHandle));
            int sumaKolejek = encoder.toInteger32(theParameters.get(federate.iloscKolejekKolejkiHandle));
            int uprzywilejowanychKolejek = encoder.toInteger32(theParameters.get(federate.iloscUprzywilejowanychKolejkiHandle));

            Object [] data = { wszyscyWKolejkach, sumaKolejek, uprzywilejowanychKolejek };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.STATYSTYKI_KOLEJKA, receiveTime));
        }

        if (interactionClass.equals(federate.statystykiKasaInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction StatystykiKasa [TIME:%.1f]", receiveTime));
            int zakupionychTowarow = encoder.toInteger32(theParameters.get(federate.zakupionychTowarowKasaHandle));
            int zwroconychTowarow = encoder.toInteger32(theParameters.get(federate.zwrotyTowarowKasaHandle));
            int skorzystaloZUprzywilejowanej = encoder.toInteger32(theParameters.get(federate.skorzystaloZUprzywilejowanej));

            Object [] data = { zakupionychTowarow, zwroconychTowarow, skorzystaloZUprzywilejowanej };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.STATYSTYKI_KASA, receiveTime));
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
