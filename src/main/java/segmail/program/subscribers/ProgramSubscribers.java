/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.program.subscribers;

import eds.component.GenericObjectService;
import eds.entity.client.VerifiedSendingAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import org.joda.time.DateTime;
import seca2.program.Program;
import segmail.entity.subscription.SUBSCRIBER_STATUS;
import segmail.entity.subscription.SUBSCRIPTION_STATUS;
import segmail.entity.subscription.SubscriberAccount;
import segmail.entity.subscription.SubscriberFieldValue;
import segmail.entity.subscription.Subscription;
import segmail.entity.subscription.SubscriptionList;
import segmail.entity.subscription.SubscriptionListField;

/**
 *
 * @author LeeKiatHaw
 */
public class ProgramSubscribers extends Program {
    
    @EJB GenericObjectService objService;
    
    // subscriber_table
    private Map<Long,Map<String,Object>> subscriberTable;
    private String checkedIds;
    
    private int currentPage;
    
    //I can't believe we have to resort to this
    //This is actually the best way to control the page numbers
    private List<Integer> pages;
    
    private List<String> assignedLists;
    private List<Long> convertedAssignedLists;
    private List<SubscriptionList> ownedLists;
    
    // FormSubscriptionLists and FormSubscriberStatus
    private List<String> subscriberStatus;
    private List<SUBSCRIBER_STATUS> convertedSubscriberStatus;
    private final SUBSCRIBER_STATUS[] allStatuses = SUBSCRIBER_STATUS.values();
    private String emailSearch;
    private String anyOrAllLists;
    private String anyOrAllStatuses;
    private Map<String,String> statusColor;
    
    //FormImportSubscribers and FormAddSubscriber
    private List<SubscriptionListField> fieldList;
    private List<SubscriptionList> selectedLists;
    private Map<String,Object> fieldValues;
    private Map<String,SubscriptionListField> fieldMap;
    
    private final String[] SCREENS = {
        "SETUP",
        "SUBSCRIBER_LIST",
        "SUBSCRIBER_VIEW",
        "SEND_EMAIL"
    };
    private String currentScreen;
    private boolean setup;
    
    // Subscriber view
    private SubscriberAccount subscriber;
    private Map<String,SubscriberFieldValue> subscriberValues; // field_key -> value
    private List<Subscription> subscriptions;
    private String subscriberAge;
    private DateTime subscribedSince;
    
    // Add Subscriber Field
    private List<SubscriptionListField> remainFields;
    
    // Send email
    private String subject;
    private String body;
    private List<VerifiedSendingAddress> verifiedAddresses;
    private String senderAddress;
    private String recipient;
    private boolean preview;
    
    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public List<VerifiedSendingAddress> getVerifiedAddresses() {
        return verifiedAddresses;
    }

    public void setVerifiedAddresses(List<VerifiedSendingAddress> verifiedAddresses) {
        this.verifiedAddresses = verifiedAddresses;
    }
    
    public DateTime getSubscribedSince() {
        return subscribedSince;
    }

    public void setSubscribedSince(DateTime subscribedSince) {
        this.subscribedSince = subscribedSince;
    }

    public String getSubscriberAge() {
        return subscriberAge;
    }

    public void setSubscriberAge(String subscriberAge) {
        this.subscriberAge = subscriberAge;
    }

    public SubscriberAccount getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(SubscriberAccount subscriber) {
        this.subscriber = subscriber;
    }

    public Map<String, SubscriberFieldValue> getSubscriberValues() {
        return subscriberValues;
    }

    public void setSubscriberValues(Map<String, SubscriberFieldValue> subscriberValues) {
        this.subscriberValues = subscriberValues;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public boolean isSetup() {
        return setup;
    }

    public void setSetup(boolean setup) {
        this.setup = setup;
    }

    public String getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(String currentScreen) {
        this.currentScreen = currentScreen;
    }

    public String[] getSCREENS() {
        return SCREENS;
    }
    
    public String getEmailSearch() {
        return emailSearch;
    }

    public void setEmailSearch(String emailSearch) {
        this.emailSearch = emailSearch;
    }
    
    public Map<String, Object> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(Map<String, Object> fieldValues) {
        this.fieldValues = fieldValues;
    }

    public List<SubscriptionList> getSelectedLists() {
        return selectedLists;
    }

    public void setSelectedLists(List<SubscriptionList> selectedLists) {
        this.selectedLists = selectedLists;
    }
    
    public List<SubscriptionListField> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<SubscriptionListField> fieldList) {
        this.fieldList = fieldList;
    }

    public List<SUBSCRIBER_STATUS> getConvertedSubscriberStatus() {
        return convertedSubscriberStatus;
    }

    public void setConvertedSubscriberStatus(List<SUBSCRIBER_STATUS> convertedSubscriberStatus) {
        this.convertedSubscriberStatus = convertedSubscriberStatus;
    }

    public SUBSCRIBER_STATUS[] getAllStatuses() {
        return allStatuses;
    }

    public List<String> getSubscriberStatus() {
        return subscriberStatus;
    }

    public void setSubscriberStatus(List<String> subscriberStatus) {
        this.subscriberStatus = subscriberStatus;
    }

    public List<Long> getConvertedAssignedLists() {
        return convertedAssignedLists;
    }

    public void setConvertedAssignedLists(List<Long> convertedAssignedLists) {
        this.convertedAssignedLists = convertedAssignedLists;
    }

    public List<String> getAssignedLists() {
        return assignedLists;
    }

    public void setAssignedLists(List<String> assignedLists) {
        this.assignedLists = assignedLists;
    }

    public List<SubscriptionList> getOwnedLists() {
        return ownedLists;
    }

    public void setOwnedLists(List<SubscriptionList> ownedLists) {
        this.ownedLists = ownedLists;
    }

    public List<Integer> getPages() {
        return pages;
    }

    public void setPages(List<Integer> pages) {
        this.pages = pages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public Map<Long,Map<String,Object>> getSubscriberTable() {
        return subscriberTable;
    }

    public void setSubscriberTable(Map<Long,Map<String,Object>> subscriberTable) {
        this.subscriberTable = subscriberTable;
    }

    public Map<String, SubscriptionListField> getFieldMap() {
        return fieldMap;
    }

    public void setFieldMap(Map<String, SubscriptionListField> fieldMap) {
        this.fieldMap = fieldMap;
    }

    public String getAnyOrAllLists() {
        return anyOrAllLists;
    }

    public void setAnyOrAllLists(String anyOrAllLists) {
        this.anyOrAllLists = anyOrAllLists;
    }

    public String getAnyOrAllStatuses() {
        return anyOrAllStatuses;
    }

    public void setAnyOrAllStatuses(String anyOrAllStatuses) {
        this.anyOrAllStatuses = anyOrAllStatuses;
    }

    public Map<String, String> getStatusColor() {
        return statusColor;
    }

    public void setStatusColor(Map<String, String> statusColor) {
        this.statusColor = statusColor;
    }

    public List<SubscriptionListField> getRemainFields() {
        return remainFields;
    }

    public void setRemainFields(List<SubscriptionListField> remainFields) {
        this.remainFields = remainFields;
    }

    public String getCheckedIds() {
        return checkedIds;
    }

    public void setCheckedIds(String checkedIds) {
        this.checkedIds = checkedIds;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public void clearVariables() {
        
    }

    @Override
    public void initRequestParams() {
        
    }

    @Override
    public void initProgram() {
        setSubscriberTable(new HashMap<Long,Map<String,Object>>());
        setCurrentPage(1);
        setAssignedLists(new ArrayList<String>());
        setConvertedAssignedLists(new ArrayList<Long>());
        setOwnedLists(new ArrayList<SubscriptionList>());
        
        setConvertedSubscriberStatus(new ArrayList<SUBSCRIBER_STATUS>());
        setSubscriberStatus(new ArrayList<String>());
        setFieldValues(new HashMap<String,Object>());
        
        setAnyOrAllLists("any");
        setAnyOrAllStatuses("any");
        
        // Initialize colors for subscriber status
        setStatusColor(new HashMap<String,String>());
        getStatusColor().put(SUBSCRIBER_STATUS.NEW.name, "warning");
        getStatusColor().put(SUBSCRIBER_STATUS.VERIFIED.name, "success");
        getStatusColor().put(SUBSCRIBER_STATUS.REMOVED.name, "default");
        getStatusColor().put(SUBSCRIBER_STATUS.UNSUBSRIBE_ALL.name, "default");
        getStatusColor().put(SUBSCRIBER_STATUS.BOUNCED.name, "danger");
        // Subscription status
        getStatusColor().put(SUBSCRIPTION_STATUS.CONFIRMED.name, "success");
        getStatusColor().put(SUBSCRIPTION_STATUS.UNSUBSCRIBED.name, "default");
        getStatusColor().put(SUBSCRIPTION_STATUS.REMOVED.name, "default");
        
    }
    
    /**
     * Common functions
     */
    
    public void loadSubscriberValues(long subscriberId) {
        // Set values/attributes
        List<SubscriberFieldValue> values = objService.getEnterpriseData(subscriberId, SubscriberFieldValue.class);
        List<String> fieldkeys = new ArrayList<>(); // This is for later
        Map<String,SubscriberFieldValue> subscriberValues = new HashMap<>();
        values.forEach(v -> {
            if(!fieldkeys.contains(v.getFIELD_KEY())) {
                fieldkeys.add(v.getFIELD_KEY());
            }
            subscriberValues.put(v.getFIELD_KEY(), v);
        });
        setSubscriberValues(subscriberValues);
    }
}
