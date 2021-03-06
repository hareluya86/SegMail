/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this autoEmail file, choose Tools | Templates
 * and open the autoEmail in the editor.
 */
package segmail.component.subscription;

import eds.component.GenericObjectService;
import eds.component.UpdateObjectService;
import eds.component.client.ClientFacade;
import eds.component.client.ClientResourceInterceptor;
import eds.component.data.EntityNotFoundException;
import eds.component.data.IncompleteDataException;
import eds.component.data.RelationshipExistsException;
import eds.entity.client.Client;
import eds.component.data.DataValidationException;
import eds.component.data.RelationshipNotFoundException;
import eds.component.encryption.EncryptionUtility;
import eds.component.encryption.EncryptionType;
import eds.component.mail.InvalidEmailException;
import eds.component.mail.MailServiceOutbound;
import eds.entity.data.EnterpriseObject;
import eds.entity.mail.Email;
import eds.entity.mail.QueuedEmail;
import java.math.BigInteger;
import segmail.entity.subscription.SubscriberAccount_;
import segmail.entity.subscription.Subscription;
import segmail.entity.subscription.SubscriptionList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.FormParam;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatterBuilder;
import segmail.component.subscription.autoresponder.AutoresponderService;
import segmail.component.subscription.mailmerge.MailMergeService;
import segmail.entity.subscription.SUBSCRIBER_STATUS;
import segmail.entity.subscription.SubscriptionListField;
import segmail.entity.subscription.SUBSCRIPTION_STATUS;
import static segmail.entity.subscription.SUBSCRIPTION_STATUS.NEW;
import static segmail.entity.subscription.SUBSCRIPTION_STATUS.CONFIRMED;
import segmail.entity.subscription.SubscriberAccount;
import segmail.entity.subscription.SubscriberCount;
import segmail.entity.subscription.SubscriberCount_;
import segmail.entity.subscription.SubscriberFieldValidationException;
import segmail.entity.subscription.SubscriberFieldValue;
import segmail.entity.subscription.SubscriberFieldValue_;
import segmail.entity.subscription.SubscriberOwnership;
import segmail.entity.subscription.SubscriberOwnership_;
import segmail.entity.subscription.Subscription_;
import segmail.entity.subscription.autoresponder.AUTO_EMAIL_TYPE;
import segmail.entity.subscription.autoresponder.AutoresponderEmail;

/**
 *
 * @author LeeKiatHaw
 */
@Stateless
@Interceptors({ClientResourceInterceptor.class})
public class SubscriptionService {

    public static final String DEFAULT_EMAIL_FIELD_NAME = "Email";
    public static final String DEFAULT_EMAIL_PATTERN = "(?i)e[-\\S]*mail";
    public static final String DEFAULT_KEY_FOR_LIST = "LIST";
    

    /**
     * Generic services
     */
    @EJB
    private GenericObjectService objService;
    @EJB
    private UpdateObjectService updService;
    @EJB
    private ListService listService;
    @EJB
    private MassSubscriptionService massSubService;

    /**
     * Delegate services
     */
    @EJB
    private AutoresponderService autoresponderService;
    @EJB
    private MailMergeService mailMergeService;
    @EJB
    private MailServiceOutbound mailService;
    
    /**
     * Helper services
     */
    @EJB SubscriptionServiceHelper helper;

    /**
     * [2015.07.12] Because the EJB Interceptor way failed, so this is a very
     * good alternative to omit clientid input for every method call.
     *
     * [2016.07.09] A service should not be injected with ClientFacade because
     * it can be also called from a webservice endpoint. Unless ClientModule
     * works in a webservice call, which is impossible because it is activated
     * by the servlet pattern "/program/*" Oh...turns out it is possible to tune
     * ClientModule to handle webservice requests. But we still can't do this
     * because webservice can be called by another system user eg. a server to
     * server call.
     */
    @Inject
    ClientFacade clientFacade;

    @Inject
    SubscriptionContainer subContainer;

    /**
     * This assumes that the client has constructed a list of
     * SubscriberFieldValue with the correct FIELD_KEY values set in these
     * objects. There should be another method that takes in just a
     * Map<String,String> of objects.
     *
     * Each email address is unique only to each Client object. For example, if
     * an email is subscribed to 2 lists from 2 different client, then it will
     * have 2 different SubscriberAccount objects.
     *
     * @param clientId
     * @param listId
     * @param values
     * @param doubleOptin
     * @return
     * @throws EntityNotFoundException if clientId or listId are not found
     * @throws IncompleteDataException if:
     * <ul>
     * <li>List doesn't have <em>any</em> fields created yet.</li>
     * <li>Email is not provided.</li>
     * <ul>
     * @throws segmail.component.subscription.SubscriptionException if
     * <ul>
     * <li>Any mandatory field is not provided, specified by
     * SubscriptionListField.MANDATORY.</li>
     * <ul>
     * @throws RelationshipExistsException if the subscriber is already
     * confirmed in the list and encapsulates the confirmation key in it
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Subscription subscribe(long clientId, long listId, Map<String, Object> values, boolean doubleOptin)
            throws EntityNotFoundException, IncompleteDataException, SubscriptionException, RelationshipExistsException {

        if(values == null || values.isEmpty())
            throw new IncompleteDataException("No fields found.");
            
        Client client = objService.getEnterpriseObjectById(clientId, Client.class); //DB hit, can be cached
        if (client == null) {
            throw new EntityNotFoundException(Client.class, clientId);
        }
        //subContainer.setClient(client);

        SubscriptionList list = objService.getEnterpriseObjectById(listId, SubscriptionList.class);
        if (list == null) {
            throw new EntityNotFoundException(SubscriptionList.class, listId);
        }
        //subContainer.setList(list);

        List<SubscriptionListField> fields = listService.getFieldsForSubscriptionList(listId);
        if (fields == null || fields.isEmpty()) {
            throw new IncompleteDataException("List " + listId + " does not have fields created.");
        }
        subContainer.setListFields(fields);

        //Look for the key in fields for DEFAULT_EMAIL_FIELD_NAME
        String email = "";
        for (SubscriptionListField field : fields) {
            if (field.getFIELD_NAME().equals(DEFAULT_EMAIL_FIELD_NAME)) {
                email = (String) values.get((String) field.generateKey());
                
                if (email == null || email.isEmpty()) {
                    throw new IncompleteDataException("Mandatory list field " + DEFAULT_EMAIL_FIELD_NAME + " is missing.");
                }
                email = email.trim(); //for checking
                break;
            }
        }
        
        SUBSCRIPTION_STATUS[] status = {NEW,CONFIRMED};
        List<Subscription> existingSubscriptions = this.getSubscriptions(email, listId, status);
        if (existingSubscriptions != null && !existingSubscriptions.isEmpty()) {
            throw new RelationshipExistsException(existingSubscriptions.get(0).getSTATUS());
        }

        List<Map<String, Object>> singleSubscriberMap = new ArrayList<>();
        singleSubscriberMap.add(values);

        Map<String, List<Map<String, Object>>> results = massSubService.massSubscribe(client, singleSubscriberMap, list, doubleOptin);
        for (String key : results.keySet()) {
            throw new SubscriptionException(key);
        }

        return this.getSubscriptions(email, listId, null).get(0);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Subscription> subscribe(long clientId, List<SubscriptionList> lists, Map<String, Object> values, boolean doubleOptin)
            throws EntityNotFoundException, IncompleteDataException, SubscriptionException, RelationshipExistsException {

        List<Subscription> results = new ArrayList<>();
        for(SubscriptionList list : lists) {
            Subscription result = this.subscribe(clientId, list.getOBJECTID(), values, doubleOptin);
            results.add(result);
        }
        
        return results;
    }
    /**
     * Check if a subscriber subscriberEmail is already subscribed to a list.
     * Each SubscriberAccount object is unique to a Client. The same email will
     * have 2 SubscriberAccount objects created if this email is subscribed to 2
     * lists from 2 different client.
     *
     * @param subscriberEmail
     * @param listId
     * @return
     */
    public boolean checkSubscribed(String subscriberEmail, long listId) {
        //Retrieving Subscriptions which has a SubscriberAccount subscriberEmail and a SubscriptionList ID
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Subscription> fromSubscr = criteria.from(Subscription.class);
        Root<SubscriberAccount> fromSubscrAcc = criteria.from(SubscriberAccount.class);

        criteria.select(builder.count(fromSubscrAcc));

        criteria.where(
                builder.and(
                        builder.equal(fromSubscr.get(Subscription_.TARGET), listId),
                        builder.equal(fromSubscr.get(Subscription_.SOURCE), fromSubscrAcc.get(SubscriberAccount_.OBJECTID)),
                        builder.equal(fromSubscrAcc.get(SubscriberAccount_.EMAIL), subscriberEmail)
                )
        );

        Long results = objService.getEm().createQuery(criteria).getSingleResult();

        return results > 0;
    }

    /**
     * **Serious bug: limit limits the num of fields, not subscriber!
     *
     * @param listId
     * @param startIndex
     * @param limit
     * @return The subscribers in a map of a map with their object IDs as
     * primary key in the first map and the list's field keys as keys in the 2nd
     * map.
     */
    public Map<Long, Map<String, String>> getSubscriberValuesMap(long listId, int startIndex, int limit) {
        
        //Get all the SubscriptionFields first
        List<String> fields = listService.getSubscriptionListFieldKeys(listId);
        //Multiply limit by number of fields, otherwise we would limit by num of fields, not num of subscribers
        int limitSubscribers = limit * fields.size();

        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery criteria = builder.createQuery(SubscriberFieldValue.class);
        Root<Subscription> fromSubscr = criteria.from(Subscription.class);
        Root<SubscriberFieldValue> fromFieldValue = criteria.from(SubscriberFieldValue.class);

        criteria.select(fromFieldValue);
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(builder.equal(fromSubscr.get(Subscription_.TARGET), listId));
        conditions.add(builder.equal(
                fromSubscr.get(Subscription_.SOURCE),
                fromFieldValue.get(SubscriberFieldValue_.OWNER)));
        if (fields != null && !fields.isEmpty()) {
            conditions.add(fromFieldValue.get(SubscriberFieldValue_.FIELD_KEY).in(fields));
        }
        criteria.where(
                builder.and(
                        conditions.toArray(new Predicate[]{})
                )
        );

        List<SubscriberFieldValue> results = objService.getEm().createQuery(criteria)
                .setFirstResult(startIndex)
                .setMaxResults(limitSubscribers)
                .getResultList();

        Map<Long, Map<String, String>> resultMap
                = new HashMap<>();

        //Collections.sort(results);//Sorting before creating the map may not be the most correct solution but it's the most efficient and effective one at the moment
        for (SubscriberFieldValue field : results) {
            SubscriberAccount subscriber = field.getOWNER();
            if (!resultMap.containsKey(subscriber.getOBJECTID())) {
                resultMap.put(subscriber.getOBJECTID(), new HashMap<String, String>());

            }
            resultMap.get(subscriber.getOBJECTID()).put(field.getFIELD_KEY(), field.getVALUE());
        }
        return resultMap;
    }

    /**
     * Non-update helper method to construct a list of SubscriberFieldValue to
     * be populated. Mainly for frontend forms usage. The returned result is not
     * ready to be updated into the DB yet because it is still missing the
     * SubscriberAccount EnterpriseObject.
     *
     * @param fields
     * @return
     */
    public Map<String, Object> constructSubscriberFieldValues(List<SubscriptionListField> fields) {
        Map<String, Object> values = new HashMap<>();

        if (fields == null || fields.isEmpty()) {
            return values;
        }

        List<SubscriptionListField> sortedFields = new ArrayList<>(fields);
        Collections.sort(sortedFields);

        for (int i = 0; i < fields.size(); i++) {
            SubscriptionListField field = fields.get(i);
            values.put(field.generateKey().toString(), "");
        }

        return values;
    }

    /**
     *
     * @param sub
     * @throws IncompleteDataException If no Send As address is set for list or
     * a confirmation email is not assigned to the list.
     * @throws eds.component.data.DataValidationException if the following error
     * occurs:<br>
     * <ul>
     * <li>List object has no SEND_AS_EMAIL set.</li>
     * <li>List object has no AutoresponderEmail of the AUTO_EMAIL_TYPE
     * CONFIRMATION</li>
     * </ul>
     * @throws InvalidEmailException if either sender's or recipients' email
     * addresses are invalid.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void sendConfirmationEmail(Subscription sub)
            throws IncompleteDataException, DataValidationException, InvalidEmailException {

        SubscriptionList list = sub.getTARGET();

        String sendAs = list.getSEND_AS_EMAIL();
        if (sendAs == null || sendAs.isEmpty()) {
            throw new IncompleteDataException("Please set \"Send As\" address before sending confirmation emails.");
        }

        //Retrieve the autoemail from list using AutoresponderService
        List<AutoresponderEmail> assignedAutoEmails = autoresponderService.getAssignedAutoEmailsForList(list.getOBJECTID(), AUTO_EMAIL_TYPE.CONFIRMATION);
        AutoresponderEmail assignedConfirmEmail = (assignedAutoEmails == null || assignedAutoEmails.isEmpty())
                ? null : assignedAutoEmails.get(0);

        if (assignedConfirmEmail == null) {
            throw new IncompleteDataException("Please assign a Confirmation email before adding subscribers.");
        }

        //Parse all mailmerge functions using MailMergeService
        String newEmailBody = assignedConfirmEmail.getBODY();
        newEmailBody = mailMergeService.parseConfirmationLink(newEmailBody, sub.getCONFIRMATION_KEY());
        newEmailBody = mailMergeService.parseForAutoresponders(newEmailBody, sub.getSOURCE(), list);
        //newEmailBody = mailMergeService.parseListAttributes(newEmailBody, listId);
        //newEmailBody = mailMergeService.parseUnsubscribeLink(newEmailBody, sub.getUNSUBSCRIBE_KEY()); //Should not be here!

        //Send the email using MailServiceOutbound
        Email confirmEmail = new QueuedEmail();
        confirmEmail.setSENDER_ADDRESS(list.getSEND_AS_EMAIL());
        confirmEmail.setSENDER_NAME(list.getSEND_AS_NAME());
        confirmEmail.setBODY(newEmailBody);
        confirmEmail.setSUBJECT(assignedConfirmEmail.getSUBJECT());
        confirmEmail.addRecipient(sub.getSOURCE().getEMAIL());

        mailService.queueEmail(confirmEmail, DateTime.now());

        sub.incrementConfirmEmail();
        sub = (Subscription) updService.merge(sub);
    }

    /**
     * Get a list of SubscriberAccount for a specific client with the given
     * email.
     *
     * @param email
     * @param clientId
     * @return
     */
    public List<SubscriberAccount> getSubscribersForClientByEmail(String email, long clientId) {
        List<String> emails = new ArrayList<>();
        emails.add(email);

        List<SubscriberAccount> results = this.getSubscribersForClientByEmails(emails, clientId);

        return results;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private SubscriberAccount getExistingOrCreateNewSubscriber(String email, long clientId) throws EntityNotFoundException {
        //Retrieve the client object
        Client client = objService.getEnterpriseObjectById(clientId, Client.class);
        if (client == null) {
            throw new EntityNotFoundException(Client.class, clientId);
        }

        return getExistingOrCreateNewSubscriber(email, client);
    }

    /**
     * Retrieves existing subscriber or create a new one using an email address
     * for a given client.
     *
     * @param email
     * @param clientId
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private SubscriberAccount getExistingOrCreateNewSubscriber(String email, Client client) throws EntityNotFoundException {
        List<SubscriberAccount> existingAccs = this.getSubscribersForClientByEmail(email, client.getOBJECTID());
        //If the email subscriber exists for the given client, return the found subscriber
        if (existingAccs != null && !existingAccs.isEmpty()) {
            return existingAccs.get(0);
        }

        //If not found, create new and assign it to the client
        SubscriberAccount newOrExistingAcc = new SubscriberAccount();
        newOrExistingAcc.setEMAIL(email);

        this.updService.getEm().persist(newOrExistingAcc);

        //Assign it to the client
        SubscriberOwnership assign = new SubscriberOwnership();
        assign.setTARGET(client);
        assign.setSOURCE(newOrExistingAcc);

        this.updService.getEm().persist(assign);

        return newOrExistingAcc;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Subscription> getSubscriptions(String subscriberEmail, long listId, SUBSCRIPTION_STATUS[] statusList) {
        CriteriaBuilder builder = this.objService.getEm().getCriteriaBuilder();
        CriteriaQuery<Subscription> query = builder.createQuery(Subscription.class);
        Root<SubscriberAccount> fromSubscriber = query.from(SubscriberAccount.class);
        Root<SubscriptionList> fromList = query.from(SubscriptionList.class);
        Root<Subscription> fromSubscription = query.from(Subscription.class);

        query.select(fromSubscription);
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(builder.equal(fromSubscriber.get(SubscriberAccount_.EMAIL), subscriberEmail));
        conditions.add(builder.equal(fromSubscription.get(Subscription_.TARGET), listId));
        conditions.add(builder.equal(fromSubscription.get(Subscription_.SOURCE), fromSubscriber.get(SubscriberAccount_.OBJECTID)));
        if (statusList != null && statusList.length > 0) {
            String[] statusNames = new String[statusList.length];
            for (int i = 0; i < statusList.length; i++) {
                statusNames[i] = statusList[i].name;
            }
            conditions.add(fromSubscription.get(Subscription_.STATUS).in(statusNames));
        }
        query.where(builder.and(
                conditions.toArray(new Predicate[]{})
        ));

        List<Subscription> results = this.objService.getEm().createQuery(query)
                .getResultList();

        return results;
    }

    /**
     * 
     * @param subscriberEmails if null or empty, an empty list will be returned.
     * @param listId
     * @param statusList Null or empty to retrieve all
     * @return 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Subscription> getSubscriptionsByEmails(List<String> subscriberEmails, long listId, SUBSCRIPTION_STATUS[] statusList) {
        if(subscriberEmails == null || subscriberEmails.isEmpty())
            return new ArrayList<>();
        
        CriteriaBuilder builder = this.objService.getEm().getCriteriaBuilder();
        CriteriaQuery<Subscription> query = builder.createQuery(Subscription.class);
        Root<SubscriberAccount> fromSubscriber = query.from(SubscriberAccount.class);
        Root<SubscriptionList> fromList = query.from(SubscriptionList.class);
        Root<Subscription> fromSubscription = query.from(Subscription.class);

        query.select(fromSubscription).distinct(true);
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(fromSubscriber.get(SubscriberAccount_.EMAIL).in(subscriberEmails));
        conditions.add(builder.equal(fromSubscription.get(Subscription_.TARGET), listId));
        conditions.add(builder.equal(fromSubscription.get(Subscription_.SOURCE), fromSubscriber.get(SubscriberAccount_.OBJECTID)));
        if (statusList != null && statusList.length > 0) {
            String[] statusNames = new String[statusList.length];
            for (int i = 0; i < statusList.length; i++) {
                statusNames[i] = statusList[i].name;
            }
            conditions.add(fromSubscription.get(Subscription_.STATUS).in(statusNames));
        }
        query.where(builder.and(
                conditions.toArray(new Predicate[]{})
        ));

        List<Subscription> results = this.objService.getEm().createQuery(query)
                .getResultList();

        return results;
    }

    /**
     *
     * @param confirmKey
     * @return
     * @throws RelationshipNotFoundException if no such Subscription is found
     * @throws IncompleteDataException if no "Send As" address is set for
     * SubscriptionList
     * @throws DataValidationException if either sender or recipient email is
     * missing.
     * @throws InvalidEmailException if email address is invalid
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Subscription confirmSubscriber(String confirmKey)
            throws RelationshipNotFoundException, IncompleteDataException, DataValidationException, InvalidEmailException {

        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<Subscription> query = builder.createQuery(Subscription.class);
        Root<Subscription> fromSubsc = query.from(Subscription.class);

        query.select(fromSubsc);
        query.where(builder.equal(fromSubsc.get(Subscription_.CONFIRMATION_KEY), confirmKey));

        List<Subscription> results = objService.getEm().createQuery(query)
                .getResultList();

        if (results == null || results.isEmpty()) {
            throw new RelationshipNotFoundException("Subscription not found for confirmation key.");
        }

        if (results.size() > 1) {
            throw new RuntimeException("SHA-256 collision! We're all going to die!!!");
        }

        Subscription sub = results.get(0);

        sub.setSTATUS(SUBSCRIPTION_STATUS.CONFIRMED.toString());
        //sub.setCONFIRMATION_KEY("");//remove confirmation key?

        //Send out welcome email (if assigned)
        sendWelcomeEmail(sub);

        sub = updService.getEm().merge(sub);
        
        // Update the SubscriberAccount STATUS
        SubscriberAccount acc = sub.getSOURCE();
        if(SUBSCRIBER_STATUS.NEW == acc.SUBSCRIBER_STATUS()) {
            acc.setSUBSCRIBER_STATUS(SUBSCRIBER_STATUS.VERIFIED.name);
            acc = updService.getEm().merge(acc);
        }

        updateSubscriberCount(sub.getTARGET(), DateTime.now());

        return sub;

    }

    /**
     * Remove the Subscription record identified by unsubKey and updates all 
     * lists counts.
     * 
     * @param unsubKey
     * @param status
     * @return
     * @throws RelationshipNotFoundException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Subscription> updateSubscription(String unsubKey, SUBSCRIPTION_STATUS status) throws RelationshipNotFoundException {

        List<Subscription> results = getSubscriptionByUnsubKey(unsubKey);

        if (results == null || results.isEmpty()) {
            throw new RelationshipNotFoundException("Subscription not found for unsubscribe key.");
        }

        List<SubscriptionList> lists = new ArrayList<>();
        for (Subscription sub : results) {
            sub.setSTATUS(status);
            sub = (Subscription) updService.getEm().merge(sub);
            if (!lists.contains(sub.getTARGET())) {
                lists.add(sub.getTARGET());
            }
        }

        for (SubscriptionList list : lists) {
            updateSubscriberCount(list, DateTime.now());
        }

        return results;
    }
    
    /**
     * This is designed to be called from a WEB server to retrigger a
     * confirmation email.
     *
     * @param key
     * @throws eds.component.data.IncompleteDataException
     * @throws eds.component.data.DataValidationException
     * @throws eds.component.mail.InvalidEmailException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void retriggerConfirmation(String key)
            throws IncompleteDataException, DataValidationException, InvalidEmailException {
        List<Subscription> subscriptions = getSubscriptionByConfirmKey(key);
        //Impossible to have a duplicate because the key was created with list id and 
        //subscriber id. 
        if (subscriptions == null || subscriptions.isEmpty()) {
            //What should we do if the key is not found?
            return;
        }

        Subscription subscription = subscriptions.get(0);

        //this.sendConfirmationEmail(subscription);
        massSubService.sendConfirmationEmails(subscriptions);
    }

    /**
     * A method which is better for Web server calls.
     *
     * @param listId
     * @param email
     * @throws RelationshipNotFoundException if the subscriber has not even
     * signed up yet.
     * @throws IncompleteDataException If no Send As address is set for list or
     * a confirmation email is not assigned to the list.
     * @throws DataValidationException if the following error occurs:<br>
     * <ul>
     * <li>List object has no SEND_AS_EMAIL set.</li>
     * <li>List object has no AutoresponderEmail of the AUTO_EMAIL_TYPE
     * CONFIRMATION</li>
     * </ul>
     * @throws InvalidEmailException if either sender's or recipients' email
     * addresses are invalid.
     */
    public void retriggerConfirmation(@FormParam("listId") long listId, @FormParam("email") String email)
            throws
            RelationshipNotFoundException,
            IncompleteDataException,
            DataValidationException,
            InvalidEmailException {
        List<Subscription> subscriptions = getSubscriptions(email, listId, null);
        if (subscriptions == null || subscriptions.isEmpty()) {
            throw new RelationshipNotFoundException(email + " is not subscribed to list " + listId + " yet.");
        }

        Subscription subscription = subscriptions.get(0);

        //Doesn't matter the status, just retrigger!
        sendConfirmationEmail(subscription);
    }

    /**
     * This is a helper method that should not be exposed.
     *
     * @param confirmKey
     * @return
     */
    private List<Subscription> getSubscriptionByConfirmKey(String confirmKey) {
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<Subscription> query = builder.createQuery(Subscription.class);
        Root<Subscription> fromSubsc = query.from(Subscription.class);

        query.where(builder.equal(fromSubsc.get(Subscription_.CONFIRMATION_KEY), confirmKey));

        List<Subscription> results = objService.getEm().createQuery(query)
                .getResultList();

        return results;
    }

    private List<Subscription> getSubscriptionByUnsubKey(String unsubKey) {
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<Subscription> query = builder.createQuery(Subscription.class);
        Root<Subscription> fromSubsc = query.from(Subscription.class);

        query.where(builder.equal(fromSubsc.get(Subscription_.UNSUBSCRIBE_KEY), unsubKey));

        List<Subscription> results = objService.getEm().createQuery(query)
                .getResultList();

        return results;
    }

    /**
     * Confirmation to each subscription for each subscriber is unique.
     *
     * @param subscriberId
     * @param listId
     * @return
     */
    public String getConfirmationHashCode(long subscriberId, long listId) {
        String confirmKey = EncryptionUtility.getHash("confirm subscription of " + subscriberId + " to list " + listId, EncryptionType.SHA256);
        return confirmKey;

    }

    /**
     * Unsubcription to each subscription for each subscriber is the same.
     *
     * @param subscriberId
     * @param listId
     * @return
     */
    public String getUnsubscribeHashCode(long subscriberId, long listId) {
        String unsubKey = EncryptionUtility.getHash("unsubscribe " + subscriberId + " from " + listId, EncryptionType.SHA256);
        return unsubKey;
    }

    /**
     * Get a list of SubscriberAccount for a specific client with the given list
     * of emails.
     *
     * @param emails
     * @param clientId
     * @return
     */
    public List<SubscriberAccount> getSubscribersForClientByEmails(List<String> emails, long clientId) {
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<SubscriberAccount> query = builder.createQuery(SubscriberAccount.class);

        Root<SubscriberAccount> fromSubscAcc = query.from(SubscriberAccount.class);
        Root<SubscriberOwnership> fromSubscOwnership = query.from(SubscriberOwnership.class);

        query.select(fromSubscAcc);
        query.where(builder.and(
                fromSubscAcc.get(SubscriberAccount_.EMAIL).in(emails),
                builder.equal(
                        fromSubscAcc.get(SubscriberAccount_.OBJECTID),
                        fromSubscOwnership.get(SubscriberOwnership_.SOURCE)
                ),
                builder.equal(fromSubscOwnership.get(SubscriberOwnership_.TARGET), clientId)
        ));

        List<SubscriberAccount> results = objService.getEm().createQuery(query)
                .getResultList();

        return results;
    }

    //@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<SubscriberFieldValue> getSubscriberValuesBySubscriberObjects(List<SubscriberAccount> subscribers) {
        List<Long> ids = objService.extractIds(subscribers);
        return getSubscriberValuesBySubscriberIds(ids);
    }
    
    public List<SubscriberFieldValue> getSubscriberValuesBySubscriberObject(SubscriberAccount subscriber) {
        List<SubscriberAccount> subscribers = new ArrayList<>();
        subscribers.add(subscriber);
        
        return getSubscriberValuesBySubscriberObjects(subscribers);
    }

    //

    //@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<SubscriberFieldValue> getSubscriberValuesBySubscriberIds(List<Long> subscribers) {
        List<SubscriberFieldValue> results = objService.getEnterpriseDataByIds(subscribers, SubscriberFieldValue.class);
        return results;
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateSubscriberCount(SubscriptionList list, DateTime dt) {
        java.sql.Date date = new java.sql.Date(dt.getMillis());
        
        // Get existing count
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<SubscriberCount> query = builder.createQuery(SubscriberCount.class);
        Root<SubscriberCount> fromCount = query.from(SubscriberCount.class);
        
        query.select(fromCount);
        query.where(builder.and(
                builder.equal(fromCount.get(SubscriberCount_.OWNER), list.getOBJECTID()),
                builder.lessThanOrEqualTo(fromCount.get(SubscriberCount_.START_DATE), date),
                builder.greaterThanOrEqualTo(fromCount.get(SubscriberCount_.END_DATE), date)
        ));
        
        List<SubscriberCount> results = objService.getEm().createQuery(query)
                .getResultList();
        
        // If an existing count exists, delimit it
        if(results != null && !results.isEmpty()) {
            SubscriberCount existing = results.get(0);
            DateTime start = new DateTime(existing.getSTART_DATE());
            DateTime end = new DateTime(existing.getEND_DATE());
            
            DateTime newEnd = dt.minusDays(1); 
            // delete the record no matter what because we can't change any pri key column
            objService.getEm().remove(existing); 
            if(!newEnd.isBefore(start)) {
                // re-insert a delimited record if start <= newEnd
                SubscriberCount replacement = new SubscriberCount();
                replacement.setSTART_DATE(existing.getSTART_DATE());
                replacement.setEND_DATE(new java.sql.Date(newEnd.getMillis()));
                replacement.setOWNER(list);
                replacement.setCOUNT(existing.getCOUNT());
                replacement.setSNO(existing.getSNO());
                
                objService.getEm().persist(replacement);
            }
        }
        // Count the number of subscribers and create a new record in all cases
        SubscriberCount newCount = new SubscriberCount();
        Map<String,Long> queriedCount = helper.getSubscriptionCounts(list.getOBJECTID());

        newCount.setCOUNT(queriedCount);
        newCount.setOWNER(list);
        objService.getEm().persist(newCount);

    }

    /**
     * Updates all SubscriptionList of a single client. Sometimes it's better to
     * have more simple queries than to have fewer complex queries and overload
     * the DB.
     *
     * @param clientId
     */
    //@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) //maybe a bug
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateAllSubscriberCountForClient(long clientId) {
        List<SubscriptionList> lists = listService.getAllListForClient(clientId);
        for (SubscriptionList list : lists) {
            updateSubscriberCount(list, DateTime.now());
        }
    }

    /**
     * A Welcome email is always optional.
     *
     * @param sub
     * @throws DataValidationException if email address is invalid
     * @throws InvalidEmailException if email address is invalid
     * @throws IncompleteDataException if landing/WEB server is not set
     */
    public void sendWelcomeEmail(Subscription sub)
            throws DataValidationException, InvalidEmailException, IncompleteDataException {
        SubscriptionList list = sub.getTARGET();

        //Retrieve the autoemail from list using AutoresponderService
        List<AutoresponderEmail> assignedAutoEmails = autoresponderService.getAssignedAutoEmailsForList(list.getOBJECTID(), AUTO_EMAIL_TYPE.WELCOME);
        AutoresponderEmail assignedWelcomeEmail = (assignedAutoEmails == null || assignedAutoEmails.isEmpty())
                ? null : assignedAutoEmails.get(0);

        if (assignedWelcomeEmail == null) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, null, "Please assign a Welcome email before adding subscribers.");
            return; //Don't do anything if no welcome emails are assigned
        }

        String sendAs = list.getSEND_AS_EMAIL();
        if (sendAs == null || sendAs.isEmpty()) {
            //This would be a serious programming error as one could not have sent out a confirmation email 
            //without a Send As email
            throw new IncompleteDataException("Please set \"Send As\" address before sending welcome emails.");
        }

        //Parse all mailmerge functions using MailMergeService
        String newEmailBody = assignedWelcomeEmail.getBODY();
        newEmailBody = mailMergeService.parseUnsubscribeLink(newEmailBody, sub.getUNSUBSCRIBE_KEY());
        newEmailBody = mailMergeService.parseForAutoresponders(newEmailBody, sub.getSOURCE(), sub.getTARGET());

        //Send the email using MailServiceOutbound
        Email welcomeEmail = new QueuedEmail();
        welcomeEmail.setSENDER_ADDRESS(list.getSEND_AS_EMAIL());
        welcomeEmail.setSENDER_NAME(list.getSEND_AS_NAME());
        welcomeEmail.setBODY(newEmailBody);
        welcomeEmail.setSUBJECT(assignedWelcomeEmail.getSUBJECT());
        welcomeEmail.addRecipient(sub.getSOURCE().getEMAIL());

        mailService.queueEmail(welcomeEmail, DateTime.now());
    }

    /**
     * 
     * 1) Updates all SubscriberAccount.SUBSCRIBER_STATUS to BOUNCED
     * 2) Updates all Subscription.SUBSCRIPTION_STATUS to BOUNCED
     * 
     * For all SubscriberAccount belonging to cientId (SubscriberOwnership->SubscriberAccount)
     * and all Subscriptions belonging to the SubscriberAccount (SubscriberAccount->Subscriptions)
     *
     * @param subscribers
     * @param clientId
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int updateSubscriptionSubscriberBounce(List<String> subscribers, long clientId) {
        if(subscribers == null || subscribers.isEmpty())
            return 0;
        
        // Native MySQL query implementation because we cannot use 
        // JOINs in JPA Criteria API for EnterpriseRelationships
        String accTable = SubscriberAccount.class.getAnnotation(Table.class).name();
        String subscTable = Subscription.class.getAnnotation(Table.class).name();
        String ownTable = SubscriberOwnership.class.getAnnotation(Table.class).name();
        String objTable = EnterpriseObject.class.getAnnotation(Table.class).name();
        
        // We hard code the field names in plain strings
        String objId = "OBJECTID";
        String source = "SOURCE";
        String target = "TARGET";
        String accStatus = "SUBSCRIBER_STATUS";
        String subscStatus = "STATUS";
        String email = "EMAIL";
        String dateChangeObj = "DATE_CHANGED";
        String dateChangeRel = "DATE_CHANGED";
        
        // Update DATE_CHANGE for native queries as listeners will not be triggered!
        DateTime now = DateTime.now();
        java.sql.Date today = new java.sql.Date(now.getMillis());
        String mySQLUpdate = 
            "update "
                + accTable + " acc " //+ "SUBSCRIBER_ACCOUNT "
                + "join "
                    + objTable
                    + " obj on acc."+objId+" = obj."+objId+" "
                + "join "
                    + ownTable
                    + " own on acc."+objId+" = own."+source+" and own."+target+" = " + clientId + " "
                + "left join "
                    + subscTable
                    + " subsc on acc."+objId+" = subsc."+source+" "
            + "set "
                + "acc."+accStatus+" = '"+SUBSCRIBER_STATUS.BOUNCED+"', "
                + "subsc."+subscStatus+" = '"+SUBSCRIPTION_STATUS.BOUNCED+"', "
                + "obj."+dateChangeObj+" = '"+today+"', "
                + "subsc."+dateChangeRel+" = '"+today+"' "
            + "where "
                + "acc." + email + " in (" 
                + subscribers.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","))
                + ")";
        Query q = objService.getEm().createNativeQuery(mySQLUpdate);
        int result = q.executeUpdate();
        
        return result;
    }
    
    /**
     *
     * @param clientId the Client OBJECT_ID
     * @param listIds list of SubscriptionList OBJECT_IDs, empty or null for all
     * @param createStart Start criteria of the SubscriberAccount.DATE_CREATED
     * field, null for 01.01.1800
     * @param createEnd End criteria of the SubscriberAccount.DATE_CREATED
     * field, null for 31.12.9999
     * @param statuses list of SUBSCRIBER_STATUS, empty or null for all
     * @param emailSearch
     * @param startIndex the first SubscriberAccount index to start retrieving
     * from (note: not SubscriberFieldValue records)
     * @param maxResults the max number of SubscriberAccount records to retrieve
     * from (note: not SubscriberFieldValue records)
     * @param anyOrAll in ANY of the given listId or in ALL of the listIds
     * @param orderBy the field name in the SubscriberAccount_ class to be sorted
     * @param asc True for ascending order, false for descending order
     * @return
     * @throws eds.component.data.DataValidationException
     * @throws java.lang.NoSuchFieldException if orderBy is invalid
     */
    public List<SubscriberAccount> getSubscribersForClient(
            long clientId,
            List<Long> listIds,
            DateTime createStart,
            DateTime createEnd,
            List<SUBSCRIBER_STATUS> statuses,
            String emailSearch,
            int startIndex,
            int maxResults,
            String anyOrAll,
            String orderBy,
            boolean asc) throws DataValidationException, NoSuchFieldException {
        
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<SubscriberAccount> query = builder.createQuery(SubscriberAccount.class);
        Root<SubscriberAccount> fromAcc = query.from(SubscriberAccount.class);
        Root<SubscriberOwnership> fromOwn = query.from(SubscriberOwnership.class);
        
        query.select(fromAcc);
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(builder.equal(fromOwn.get(SubscriberOwnership_.TARGET), clientId));
        conditions.add(builder.equal(fromOwn.get(SubscriberOwnership_.SOURCE), fromAcc.get(SubscriberAccount_.OBJECTID)));
        if(createStart != null) {
            conditions.add(builder.greaterThanOrEqualTo(fromAcc.get(SubscriberAccount_.DATE_CREATED), 
                new java.sql.Date(createStart.getMillis())));
        }
        if(createEnd != null) {
            conditions.add(builder.lessThanOrEqualTo(fromAcc.get(SubscriberAccount_.DATE_CREATED), 
                new java.sql.Date(createEnd.getMillis())));
        }
        
        // Any or All fields
        if(listIds != null && !listIds.isEmpty()) {
            // Impt to be declared here instead of outside because it will add an unnecessary join.
            // If the FROM table is optional, do not declare it if not required at all!
            Root<Subscription> fromSubsc = query.from(Subscription.class); 
            conditions.add(fromSubsc.get(Subscription_.TARGET).in(listIds));
            conditions.add(builder.equal(fromSubsc.get(Subscription_.SOURCE),fromAcc.get(SubscriberAccount_.OBJECTID)));
            
            if(anyOrAll != null && "all".equalsIgnoreCase(anyOrAll)) {
                query.groupBy(fromAcc.get(SubscriberAccount_.OBJECTID));
                query.having(builder.equal(builder.count(fromAcc),listIds.size()));
            }
        }
        
        if(statuses != null && !statuses.isEmpty()) {
            List<String> statusStrings = statuses.stream().map(s -> s.name).collect(toList());
            conditions.add(fromAcc.get(SubscriberAccount_.SUBSCRIBER_STATUS).in(statusStrings));
        }
        
        // Order by 
        if (orderBy != null && SubscriberAccount_.class.getField(orderBy) != null) {
            if(asc) {
                query.orderBy(builder.asc(fromAcc.get(orderBy)));
            } else {
                query.orderBy(builder.desc(fromAcc.get(orderBy)));
            }
        }
        query.where(builder.and(conditions.toArray(new Predicate[]{})));
        List<SubscriberAccount> results = objService.getEm().createQuery(query)
                .setFirstResult(startIndex)
                .setMaxResults(maxResults)
                .getResultList();

        return results ;//subscribers;
    }

    /**
     * Aggregate multiple simple, segmented queries until the final result is
     * computed.
     *
     * @param clientId
     * @param listIds
     * @param createStart
     * @param createEnd
     * @param statuses
     * @param emailSearch
     * @param anyOrAll
     * @return
     * @throws eds.component.data.DataValidationException
     */
    public long countNumberSubscribers(
            long clientId,
            final List<Long> listIds,
            DateTime createStart,
            DateTime createEnd,
            List<SUBSCRIBER_STATUS> statuses,
            String emailSearch,
            String anyOrAll
    ) throws DataValidationException {
        
        String sql = buildDripQueryCount(clientId, listIds, createStart, createEnd, statuses, emailSearch, anyOrAll);
        
        Long result =  ((BigInteger)objService.getEm().createNativeQuery(sql)
                .getSingleResult())
                .longValue();
        
        return result;
    }
    
    private String buildDripQueryCount(
            long clientId,
            List<Long> listIds,
            DateTime createStart,
            DateTime createEnd,
            List<SUBSCRIBER_STATUS> statuses,
            String emailSearch,
            String anyOrAll
            ) throws DataValidationException {
        String sql = buildDripQuery(clientId, listIds, createStart, createEnd, statuses, emailSearch, anyOrAll);
        
        String preSQL = "SELECT COUNT(*) from (";
        String postSQL = ") a";
        
        String finalSQL = preSQL + sql + postSQL;
        return finalSQL;
            
    }
    
    private String buildDripQuery(
            long clientId,
            List<Long> listIds,
            DateTime createStart,
            DateTime createEnd,
            List<SUBSCRIBER_STATUS> statuses,
            String emailSearch,
            //String selectFunction,
            String anyOrAll
            ) throws DataValidationException {
        if (createEnd != null && createStart != null && createEnd.getMillis() < createStart.getMillis()) {
            throw new DataValidationException("End datetime is before Start dateTime");
        }
        //Risky as the table names might be changed from the Java class but we don't know which methods have hardcoded the names
        String accTable = SubscriberAccount.class.getAnnotation(Table.class).name();
        String ownTable = SubscriberOwnership.class.getAnnotation(Table.class).name();//"SUBSCRIBER_OWNERSHIP";//
        String subscTable = Subscription.class.getAnnotation(Table.class).name();//"SUBSCRIPTION";//
            
        String sql = "SELECT ";
        //sql += (selectFunction == null || selectFunction.isEmpty()) ? "a " : selectFunction+"(a) ";
        sql += "a.* ";
        sql += "FROM ";
        sql += accTable + " as a , ";
        sql += ownTable + " as b "; //join with SubscriberOwnership
        if(listIds != null && !listIds.isEmpty()) { //join with Subscription
            sql += ", " + subscTable + " as c";
        } 
        
        //Mandatory criteria: clientId
        sql += " WHERE b.TARGET = " + clientId;
        sql += " AND a.OBJECTID = b.SOURCE";
        
        if(createStart != null) {
            String dateStart = createStart.toString("YYYYMMDD");
            sql += " AND a.DATE_CREATED >= " +  dateStart;
        }
        if(createEnd != null) {
            String dateEnd = createEnd.toString("YYYYMMDD");
            sql += " AND a.DATE_CREATED <= " +  dateEnd;
        }
        if(statuses != null && !statuses.isEmpty()) {
            String statusString = "";
            for(SUBSCRIBER_STATUS status : statuses) {
                if(!statusString.isEmpty())
                    statusString += ",";
                statusString += "'"+status.name+"'";
            }
            sql += " AND a.SUBSCRIBER_STATUS in (" +  statusString +")";   
        }
        if(listIds != null && !listIds.isEmpty()) {
            String listString = "";
            for(long listId : listIds) {
                if(!listString.isEmpty()) 
                    listString += ",";
                listString += listId;
            }
            sql += " AND a.OBJECTID = c.SOURCE";
            sql += " AND c.TARGET in (" +  listString +")";   
        }
        if(emailSearch != null && !emailSearch.isEmpty()) {
            String searchString = emailSearch.replace('*', '%').replace(" OR ", " "); //Most common wildcard char people will use
            sql += " AND a.EMAIL LIKE '%"+searchString+"%'";
        }
        if(anyOrAll != null && "all".equalsIgnoreCase(anyOrAll)) {
            sql += " GROUP BY a.OBJECTID HAVING count(a.OBJECTID) = " + listIds.size();
        }
        
        return sql;
    }

    /**
     * Based on a list of fields, get SubscriptionListField->SubscriberFieldValue
     * 
     * @param fields
     * @return 
     */
    public List<SubscriberFieldValue> getRandomValues(List<SubscriptionListField> fields) {
        final int MAX = 100;
        
        List<SubscriberFieldValue> finalResults = new ArrayList<>();
        
        for(SubscriptionListField field : fields) {
            int rand = (int) (Math.random()*MAX);
            String queryString = "";
            queryString += "SELECT * FROM "
                    + SubscriberFieldValue.class.getName()
                    + " a ";

            String inCond = "WHERE a.FIELD_KEY in ('" + field.generateKey() + "')";

            Query query = objService.getEm().createQuery(inCond, SubscriberFieldValue.class)
                    .setFirstResult(0)
                    .setMaxResults(MAX);

            List<SubscriberFieldValue> results = query.getResultList();
            if(results == null || results.isEmpty())
                continue;
            
            SubscriberFieldValue randChosen = results.get((int) (results.size()*Math.random()));
            finalResults.add(randChosen);
        }
        
        return finalResults;
    }
    
    /**
     * Returns the next (size) subscribers of a list sorted by the EMAIL field ascending.
     * 
     * @param listId
     * @param start
     * @param size
     * @param statusList leave empty or null to omit this criteria
     * @return 
     */
    public List<SubscriberAccount> getNextNSubscribers(long listId, int start, int size, SUBSCRIPTION_STATUS[] statusList) {
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaQuery<SubscriberAccount> query = builder.createQuery(SubscriberAccount.class);
        Root<SubscriberAccount> fromAcc = query.from(SubscriberAccount.class);
        Root<Subscription> fromSubsc = query.from(Subscription.class);
        
        query.select(fromAcc);
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(builder.equal(fromSubsc.get(Subscription_.TARGET), listId));
        conditions.add(builder.equal(fromAcc.get(SubscriberAccount_.OBJECTID), fromSubsc.get(Subscription_.SOURCE)));
        if(statusList != null && statusList.length > 0) {
            String[] statusNames = new String[statusList.length];
            for (int i = 0; i < statusList.length; i++) {
                statusNames[i] = statusList[i].name;
            }
            conditions.add(fromSubsc.get(Subscription_.STATUS).in(statusNames));
        }
        query.where(builder.and(conditions.toArray(new Predicate[]{})));
        query.orderBy(builder.asc(fromAcc.get(SubscriberAccount_.EMAIL)));
        
        List<SubscriberAccount> results = objService.getEm().createQuery(query)
                .setFirstResult(start)
                .setMaxResults(size)
                .getResultList();
                
        
        return results;
    }
    
    /**
     * Note:
     * - Converts all email addresses to lowercases
     * @param subscribers
     * @param emailKey
     * @return 
     */
    public Collection<Map<String, Object>> mergeDuplicates(List<Map<String, Object>> subscribers, String emailKey) {
        Map<String,Map<String, Object>> results = new HashMap<>();
        
        for(Map<String, Object> subscriber : subscribers) {
            
            String email = ((String) subscriber.get(emailKey)).toLowerCase();
            Map<String, Object> newSubscriber = new HashMap<>();
            if (results.containsKey(email)) {
                //If results already contain the subscriber, retrieve it
                newSubscriber = results.get(email);
            }
            
            for(String key : subscriber.keySet()) {
                //Just dump everything, override any existing fields and adding new fields
                newSubscriber.put(key, subscriber.get(key));
            }
            
            results.put(email, newSubscriber);
        }
        
        return results.values();
    }
    
    /**
     * WARNING: No VALIDATIONS here!
     * You are inserting the value of the field AS-IS.
     * 
     * @param subscriberId
     * @param fieldKey
     * @param fieldValue
     * @param dt time of update
     * @return 
     * @throws eds.component.data.EntityNotFoundException 
     * @throws segmail.entity.subscription.SubscriberFieldValidationException 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int updateFieldValue(long subscriberId, String fieldKey, String fieldValue, DateTime dt) 
            throws EntityNotFoundException, SubscriberFieldValidationException {
        // Read the SubscriptionListField first to get the TYPE
        List<String> keys = new ArrayList<>();
        keys.add(fieldKey);
        List<SubscriptionListField> fields = listService.getFieldsByKeyOrLists(keys, null);
        if(fields == null || fields.isEmpty())
            throw new EntityNotFoundException("Field key " + fieldKey + " not found.");
        
        SubscriptionListField field = fields.get(0);
        field.TYPE().validate(fieldValue);
        
        CriteriaBuilder builder = objService.getEm().getCriteriaBuilder();
        CriteriaUpdate<SubscriberFieldValue> query = builder.createCriteriaUpdate(SubscriberFieldValue.class);
        Root<SubscriberFieldValue> fromValue = query.from(SubscriberFieldValue.class);
        
        query.set(fromValue.get(SubscriberFieldValue_.VALUE), fieldValue);
        query.set(fromValue.get(SubscriberFieldValue_.DATE_CHANGED), new java.sql.Date(dt.getMillis()));
        
        query.where(builder.and(
                builder.equal(fromValue.get(SubscriberFieldValue_.OWNER), subscriberId),
                builder.equal(fromValue.get(SubscriberFieldValue_.FIELD_KEY), fieldKey)
        ));
        
        int result = objService.getEm().createQuery(query)
                .executeUpdate();
        
        return result;
                
    }
    
    public String calculateSubscriberAge(SubscriberAccount subscriber, DateTime now) {
        Period length = (new Period(
                new DateTime(subscriber.getDATE_CREATED().getTime()),
                now
        ));
        length = length.withHours(0).withMinutes(0).withSeconds(0).withMillis(0);
        String ageString = length.toString(
                new PeriodFormatterBuilder()
                        .printZeroAlways()
                        .printZeroRarelyLast()
                .appendYears().appendSuffix(" year"," years")
                .appendSeparator(" ")
                .appendMonths().appendSuffix(" month"," months")
                .appendSeparator(" ")
                .appendWeeks().appendSuffix(" week", " weeks")
                .appendSeparator(" ")        
                .appendDays().appendSuffix(" day"," days")
                .toFormatter()
        );
        
        return ageString;
    }
    
}
