package federates.shop;

import events.ExternalEvent;
import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import utils.EncoderDecoder;
import utils.Logger;
import utils.TimeUtils;

import java.util.ArrayList;

public class ShopFederateAmbassador extends NullFederateAmbassador {
    private static final Logger logger = new Logger("ShopFederateAmbassador");
    private EncoderDecoder encoder;
    private ShopFederate federate;

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    private ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

    public ShopFederateAmbassador(ShopFederate federate) throws RTIexception {
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
        if( label.equals(ShopFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failedToSyncSet) throws FederateInternalError {
        logger.info( "Federation Synchronized: " + label );
        if( label.equals(ShopFederate.READY_TO_RUN) )
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
        if (interactionClass.equals(federate.clientEnteredShopInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            String clientId = encoder.toString(theParameters.get(federate.clientIdEnteredHandle));
            logger.info(String.format("Receive interaction ClientEnteredShop [TIME:%.1f]", receiveTime));

            externalEvents.add(new ExternalEvent(clientId, ExternalEvent.EventType.CLIENT_ENTERED_SHOP, receiveTime));
        }

        if (interactionClass.equals(federate.shoppingFinishedInteractionHandle)) {
            double receiveTime = TimeUtils.convertTime(time);
            logger.info(String.format("Receive interaction ShoppingFinished [TIME:%.1f]", receiveTime));
            String clientId = encoder.toString(theParameters.get(federate.clientIdShoppingFinishedHandle));

            externalEvents.add(new ExternalEvent(clientId, ExternalEvent.EventType.SHOPPING_FINISHED, receiveTime));
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
