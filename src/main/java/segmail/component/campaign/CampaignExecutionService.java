/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.component.campaign;

import eds.component.GenericObjectService;
import eds.component.UpdateObjectService;
import eds.component.batch.StopNextRunQuickAndDirty;
import eds.component.client.ClientFacade;
import eds.component.data.EntityNotFoundException;
import eds.component.data.IncompleteDataException;
import eds.component.data.RelationshipNotFoundException;
import eds.entity.client.Client;
import eds.entity.mail.Email;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import segmail.component.subscription.SubscriptionService;
import segmail.component.subscription.mailmerge.MailMergeService;
import segmail.entity.campaign.Assign_Campaign_Activity;
import segmail.entity.campaign.Assign_Campaign_Activity_;
import segmail.entity.campaign.Assign_Campaign_Client;
import segmail.entity.campaign.Assign_Campaign_List;
import segmail.entity.campaign.Assign_Campaign_List_;
import segmail.entity.campaign.Campaign;
import segmail.entity.campaign.CampaignActivity;
import segmail.entity.campaign.CampaignExecutionError;
import segmail.entity.campaign.Trigger_Email_Activity;
import segmail.entity.campaign.Trigger_Email_Activity_;
import segmail.entity.subscription.SubscriberAccount;
import segmail.entity.subscription.SubscriberAccount_;
import segmail.entity.subscription.SubscriberOwnership;
import segmail.entity.subscription.SubscriberOwnership_;
import segmail.entity.subscription.Subscription;
import segmail.entity.subscription.SubscriptionList;
import segmail.entity.subscription.Subscription_;

/**
 *
 * @author LeeKiatHaw
 */
@Stateless
public class CampaignExecutionService {

    public final int BATCH_SIZE = 20;
    
    @Inject ClientFacade clientFacade;

    @EJB
    GenericObjectService objService;
    @EJB
    UpdateObjectService updService;
    @EJB
    CampaignService campService;
    @EJB
    MailMergeService mmService;
    @EJB
    SubscriptionService subService;

    /**
     *
     *
     * @param campaignActivityId
     * @return
     */
    /*@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public CampaignActivityExecution createActivityExecutionSchedule(long campaignActivityId, List<Long> targetedLists) {

        CampaignActivityExecution newExecution = new CampaignActivityExecution();
        newExecution.setCAMPAIGN_ACTIVITY_ID(campaignActivityId);

        String targetedListsString = "";
        for (Long list : targetedLists) {
            if (!targetedListsString.isEmpty()) {
                targetedListsString += ",";
            }
            targetedListsString += list.toString();
        }

        newExecution.setTARGETED_LIST_ID(targetedListsString);

        objService.getEm().persist(newExecution);

        return newExecution;
    }*/

    /**
     * Executes the campaign activity from the [start]th subscriber to
     * [start]+size th subscriber.
     *
     * @param campaignActivityId
     * @param maxSize
     * @param startIndex
     * @param size
     * @return 
     * @throws eds.component.data.EntityNotFoundException
     * @throws eds.component.data.RelationshipNotFoundException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public StopNextRunQuickAndDirty executeCampaignActivity(long campaignActivityId, final int maxSize)
            throws EntityNotFoundException, RelationshipNotFoundException {
        
        CampaignActivity campaignActivity = campService.getCampaignActivity(campaignActivityId); //DB hit
        if (campaignActivity == null) {
            throw new EntityNotFoundException(CampaignActivity.class, campaignActivityId);
        }
        
        List<Campaign> campaign = objService.getAllSourceObjectsFromTarget(campaignActivityId, Assign_Campaign_Activity.class, Campaign.class);
        if (campaign == null || campaign.isEmpty())
            throw new RelationshipNotFoundException("CampaignActivity "+campaignActivityId+" is not assigned to any Campaign.");
        
        List<SubscriptionList> targetLists = objService.getAllTargetObjectsFromSource(campaign.get(0).getOBJECTID(), Assign_Campaign_List.class, SubscriptionList.class);
        if (targetLists == null || targetLists.isEmpty())
            throw new RelationshipNotFoundException("Campaign "+campaign.get(0).getOBJECTID()+" is not assigned any target lists.");
        
        List<Client> clientLists = objService.getAllTargetObjectsFromSource(campaign.get(0).getOBJECTID(), Assign_Campaign_Client.class, Client.class);
        if (clientLists == null || clientLists.isEmpty())
            throw new RelationshipNotFoundException("Campaign "+campaign.get(0).getOBJECTID()+" is not assigned to any Clients.");
        
        int count = 0; //Number 
        
        while (count < maxSize) {
            //Retrieve all subscriber emails
            List<SubscriberAccount> subscribers = 
                    getUnsentSubscriberEmailsForCampaign(campaignActivityId, 0, (int) Math.min(maxSize-count,BATCH_SIZE)); //DB hit
            //Lock 'em!
            subscribers = updService.lockObjects(subscribers, LockModeType.PESSIMISTIC_WRITE);
            //Skip if there are no more subscribers to be sent to
            if(subscribers.isEmpty())
                break;
            //Retrieve all unsubscribe codes
            Map<SubscriberAccount,String> unsubCodes = this.getUnsubscribeCodes(subscribers, clientLists.get(0).getOBJECTID()); //DB hit
            
            for (SubscriberAccount subscriber : subscribers) {

                Email email = new Email();
                try {
                    
                    email.setSUBJECT(campaignActivity.getACTIVITY_NAME());
                    email.addRecipient(subscriber.getEMAIL());
                    
                    String content = campaignActivity.getACTIVITY_CONTENT();
                    content = mmService.parseUnsubscribeLink(content, unsubCodes.get(subscriber));
                    
                    email.setBODY(content);
                    
                    objService.getEm().persist(email);
                    
                    Trigger_Email_Activity trigger = new Trigger_Email_Activity();
                    trigger.setTRIGGERED_TRANSACTION(email);
                    trigger.setTRIGGERING_OBJECT(campaignActivity);
                    
                    objService.getEm().persist(trigger);
                    
                } catch (IncompleteDataException ex) {
                    CampaignExecutionError error = new CampaignExecutionError();
                    error.setCAMPAIGN_ACTIVITY_ID(campaignActivityId);
                    error.setERROR_MESSAGE(ex.getMessage());
                    error.setRECIPIENT(subscriber.getEMAIL());
                    
                    objService.getEm().persist(error);
                } finally {
                    count++;
                }
            }
            objService.getEm().flush();
        }
        //Ugly HACK, but simplest solution
        return (count < maxSize) ? new StopNextRunQuickAndDirty() : null;

    }

    public List<String> getSubscriberEmailsForCampaign(long campaignId, int startIndex, int size) {
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<SubscriberAccount> fromSubAcc = query.from(SubscriberAccount.class);
        Root<Subscription> fromSubp = query.from(Subscription.class);
        Root<Assign_Campaign_List> fromAssignCampList = query.from(Assign_Campaign_List.class);

        query.select(fromSubAcc.get(SubscriberAccount_.EMAIL));
        query.where(
                builder.and(
                        builder.equal(fromAssignCampList.get(Assign_Campaign_List_.SOURCE), campaignId),
                        builder.equal(fromAssignCampList.get(Assign_Campaign_List_.TARGET), fromSubp.get(Subscription_.TARGET)),
                        builder.equal(fromSubp.get(Subscription_.TARGET), fromSubAcc.get(SubscriberAccount_.OBJECTID))
                )
        );

        List<String> results = objService.getEm().createQuery(query)
                .setFirstResult(startIndex)
                .setMaxResults(size)
                .getResultList();

        return results;

    }

    /**
     * A long query but worth the performance. If a subscriber appears in more
     * than 1 list, only 1 email will be sent. 1 Email per CampaignActivity per
     * SubscriberAccount.
     *
     * @param campaignActivityId
     * @param startIndex
     * @param size
     * @return
     */
    public List<SubscriberAccount> getUnsentSubscriberEmailsForCampaign(long campaignActivityId, int startIndex, int size) {
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<SubscriberAccount> query = builder.createQuery(SubscriberAccount.class);
        Root<SubscriberAccount> fromSubAcc = query.from(SubscriberAccount.class);
        Root<Subscription> fromSubp = query.from(Subscription.class);
        Root<Assign_Campaign_List> fromAssignCampList = query.from(Assign_Campaign_List.class);
        Root<Assign_Campaign_Activity> fromAssignCampAct = query.from(Assign_Campaign_Activity.class);

        //Subquery
        Subquery<String> emailQuery = query.subquery(String.class);
        Root<Trigger_Email_Activity> fromTrigger = emailQuery.from(Trigger_Email_Activity.class);
        emailQuery.select(fromTrigger.get(Trigger_Email_Activity_.SUBCRIBER_EMAIL));

        emailQuery.where(
                builder.and(
                        builder.equal(fromTrigger.get(Trigger_Email_Activity_.TRIGGERING_OBJECT), campaignActivityId)
                )
        );

        query.select(fromSubAcc);
        query.distinct(true);
        query.where(
                builder.and(
                        builder.equal(fromAssignCampAct.get(Assign_Campaign_Activity_.TARGET), campaignActivityId),
                        builder.equal(fromAssignCampList.get(Assign_Campaign_List_.SOURCE), fromAssignCampAct.get(Assign_Campaign_Activity_.SOURCE)),
                        builder.equal(fromAssignCampList.get(Assign_Campaign_List_.TARGET), fromSubp.get(Subscription_.TARGET)),
                        builder.equal(fromSubp.get(Subscription_.SOURCE), fromSubAcc.get(SubscriberAccount_.OBJECTID)),
                        builder.not(fromSubAcc.get(SubscriberAccount_.EMAIL).in(emailQuery))
                )
        );

        List<SubscriberAccount> results = objService.getEm().createQuery(query)
                .setFirstResult(startIndex)
                .setMaxResults(size)
                .getResultList();

        return results;
    }


    public List<Trigger_Email_Activity> getEmailTriggers(long campaignActivityId, String email) {
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<Trigger_Email_Activity> query = builder.createQuery(Trigger_Email_Activity.class);
        Root<Trigger_Email_Activity> fromTrigger = query.from(Trigger_Email_Activity.class);
        query.select(fromTrigger);

        query.where(
                builder.and(
                        builder.equal(fromTrigger.get(Trigger_Email_Activity_.TRIGGERING_OBJECT), campaignActivityId),
                        builder.equal(fromTrigger.get(Trigger_Email_Activity_.SUBCRIBER_EMAIL), email)
                )
        );

        List<Trigger_Email_Activity> results = objService.getEm().createQuery(query)
                .getResultList();

        return results;
    }
    
    /**
     * By using a map, this actually guarantees the uniqueness of the results. 
     * 1 subcriber to 1 code. If the subscriber is subscribed to multiple lists,
     * only the last subscription will be returned. If they click on any links 
     * with any of their own unsubscribe codes, they will be able to see all the 
     * lists that they are subscribed to and select which ones they want to unsubscribe
     * from (lists from the same client of course). 
     * 
     * @param subscribers
     * @param clientId
     * @return 
     */
    public Map<SubscriberAccount,String> getUnsubscribeCodes(List<SubscriberAccount> subscribers, long clientId) {
        //Simplest and least expensive solution.
        if(subscribers == null || subscribers.isEmpty())
            return new HashMap<>();
        
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery( Object[].class );
        Root<Subscription> fromSubscrp = query.from(Subscription.class);
        Root<SubscriberAccount> fromSubscrb = query.from(SubscriberAccount.class);
        Root<SubscriberOwnership> fromOwner = query.from(SubscriberOwnership.class);
        
        query.multiselect(fromSubscrb,fromSubscrp.get(Subscription_.UNSUBSCRIBE_KEY));
        
        query.where(builder.and(fromSubscrb.in(subscribers),
                        builder.equal(fromSubscrb.get(SubscriberAccount_.OBJECTID), fromSubscrp.get(Subscription_.SOURCE)),
                        builder.equal(fromSubscrb.get(SubscriberAccount_.OBJECTID), fromOwner.get(SubscriberOwnership_.SOURCE)),
                        builder.equal(fromOwner.get(SubscriberOwnership_.TARGET), clientId)
                )
        );
        List<Object[]> results = objService.getEm().createQuery(query)
                .getResultList();
        
        Map<SubscriberAccount,String> resultMap = new HashMap<>();
        for(Object[] result : results){
            resultMap.put((SubscriberAccount)result[0], result[1].toString());
        }
        
        return resultMap;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void executeAllCampaignActivities() {
        
    }
    
}
