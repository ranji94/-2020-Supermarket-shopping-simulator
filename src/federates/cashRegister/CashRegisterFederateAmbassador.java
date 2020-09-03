package federates.cashRegister;

import events.ExternalEvent;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import utils.EncoderDecoder;
import utils.Logger;
import utils.TimeUtils;

import java.util.ArrayList;

public class CashRegisterFederateAmbassador extends NullFederateAmbassador {
    private static final Logger logger = new Logger("CashRegisterFederateAmbassador");
    private EncoderDecoder encoder;
    private CashRegisterFederate federate;

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

    public CashRegisterFederateAmbassador(CashRegisterFederate federate) throws RTIexception {
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
        if( label.equals(CashRegisterFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failedToSyncSet) throws FederateInternalError {
        logger.info( "Federation Synchronized: " + label );
        if( label.equals(CashRegisterFederate.READY_TO_RUN) )
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
        if (interactionClass.equals(federate.openCashRegisterInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction OpenCashRegister [TIME:%.1f]", receiveTime));

            String cashRegisterId = encoder.toString(theParameters.get(federate.cashRegisterIdOpenCashRegisterHandle));
            boolean privileged = encoder.toBoolean(theParameters.get(federate.privilegedOpenCashRegisterHandle));
            int serviceTime = encoder.toInteger32(theParameters.get(federate.serviceTimeOpenCashRegisterHandle));
            Object [] data = { cashRegisterId, privileged, serviceTime };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.OPEN_CASH_REGISTER, receiveTime));
        }

        if (interactionClass.equals(federate.closeCashRegisterInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction CloseCashRegister [TIME:%.1f]", receiveTime));
            String cashRegisterId = encoder.toString(theParameters.get(federate.cashRegisterIdCloseCashRegisterHandle));

            externalEvents.add(new ExternalEvent(cashRegisterId, ExternalEvent.EventType.CLOSE_CASH_REGISTER, receiveTime));
        }

        if (interactionClass.equals(federate.clientToCashRegisterInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction ClientToCashRegister [TIME:%.1f]", receiveTime));

            String cashRegisterId = encoder.toString(theParameters.get(federate.cashRegisterIdClientToCashRegisterHandle));
            String clientId = encoder.toString(theParameters.get(federate.clientIdClientToCashRegisterHandle));
            int totalProductsCount = encoder.toInteger32(theParameters.get(federate.totalProductsClientToCashRegisterHandle));
            Object [] data = { cashRegisterId, clientId, totalProductsCount };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.CLIENT_TO_CASH_REGISTER, receiveTime));
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
