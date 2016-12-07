/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.program.subscribers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import seca2.program.Program;
import segmail.entity.subscription.SUBSCRIBER_STATUS;
import segmail.entity.subscription.SubscriptionList;
import segmail.entity.subscription.SubscriptionListField;

/**
 *
 * @author LeeKiatHaw
 */
public class ProgramSubscribers extends Program {
    
    private Map<Long,Map<String,Object>> subscriberTable;
    
    private int currentPage;
    
    //I can't believe we have to resort to this
    //This is actually the best way to control the page numbers
    private List<Integer> pages;
    
    private List<String> assignedLists;
    private List<Long> convertedAssignedLists;
    private List<SubscriptionList> ownedLists;
    
    private List<String> subscriberStatus;
    private List<SUBSCRIBER_STATUS> convertedSubscriberStatus;
    private final SUBSCRIBER_STATUS[] allStatuses = SUBSCRIBER_STATUS.values();
    private String emailSearch;
    
    //FormImportSubscribers and FormAddSubscriber
    private List<SubscriptionListField> fieldList;
    private List<SubscriptionList> selectedLists;
    private Map<String,Object> fieldValues;

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
    }
    
}
