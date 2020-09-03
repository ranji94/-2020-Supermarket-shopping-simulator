package federates.appInterface;

import events.ExternalEvent;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import utils.EncoderDecoder;
import utils.Logger;
import utils.TimeUtils;

import java.util.ArrayList;

public class AppInterfaceFederateAmbassador extends NullFederateAmbassador {
    private static final Logger logger = new Logger("AppInterfaceFederateAmbassador");
    private EncoderDecoder encoder;
    private AppInterfaceFederate federate;

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

    public AppInterfaceFederateAmbassador(AppInterfaceFederate federate) throws RTIexception {
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
        if( label.equals(AppInterfaceFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failedToSyncSet) throws FederateInternalError {
        logger.info( "Federation Synchronized: " + label );
        if( label.equals(AppInterfaceFederate.READY_TO_RUN) )
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

        if (interactionClass.equals(federate.shopStatsInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction ShopStats [TIME:%.1f]", receiveTime));
            int totalClientsCount = encoder.toInteger32(theParameters.get(federate.allClientsInShopHandle));
            int totalClientsInShopping = encoder.toInteger32(theParameters.get(federate.clientsShoppingHandle));
            int totalClientsInCashQueues = encoder.toInteger32(theParameters.get(federate.clientsInCashQueuesHandle));
            int totalClientsServiced = encoder.toInteger32(theParameters.get(federate.clientsAfterShoppingCountHandle));

            Object [] data = { totalClientsCount, totalClientsInShopping, totalClientsInCashQueues, totalClientsServiced };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.SHOP_STATS, receiveTime));
        }

        if (interactionClass.equals(federate.cashQueueStatsInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction CashQueueStats [TIME:%.1f]", receiveTime));
            String cashQueuesVisualisation = encoder.toString(theParameters.get(federate.clientsCashQueueStatsHandle));
            int totalCashQueuesCount = encoder.toInteger32(theParameters.get(federate.queuesCountCashQueueStatsHandle));
            int privilegedCashQueuesCount = encoder.toInteger32(theParameters.get(federate.privilegedCashQueueStatsHandle));

            Object [] data = { cashQueuesVisualisation, totalCashQueuesCount, privilegedCashQueuesCount };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.CASH_QUEUE_STATS, receiveTime));
        }

        if (interactionClass.equals(federate.cashRegisterStatsInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction CashRegisterStats [TIME:%.1f]", receiveTime));
            int productsBought = encoder.toInteger32(theParameters.get(federate.boughtProductsCashRegisterHandle));
            int productsReturned = encoder.toInteger32(theParameters.get(federate.returnedProductsCashRegisterHandle));
            int clientsUsedPrivilegedCashRegisterCount = encoder.toInteger32(theParameters.get(federate.usedPrivilegedCashRegisterHandle));

            Object [] data = { productsBought, productsReturned, clientsUsedPrivilegedCashRegisterCount };

            externalEvents.add(new ExternalEvent(data, ExternalEvent.EventType.CASH_REGISTER_STATS, receiveTime));
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
