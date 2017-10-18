/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eds.entity.mail;

import eds.entity.transaction.EnterpriseTransaction;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 *
 * @author LeeKiatHaw
 */
@Entity
@Table(name="EMAIL"
        ,indexes={
            @Index(name="MailServiceOutbound",
                    columnList="TRANSACTION_ID,PROCESSING_STATUS,SCHEDULED_DATETIME,DATETIME_CHANGED")
        })
public class Email extends EnterpriseTransaction {
    
    public static final String CREATED_FROM = "CREATED_FROM";
    
    private String SUBJECT;
    
    private String BODY;
    
    private String SENDER_ADDRESS;
    
    private String SENDER_NAME;
    
    private Set<String> RECIPIENTS = new HashSet();
    
    private Set<String> REPLY_TO_ADDRESSES = new HashSet();
    
    private int RETRIES;
    
    private String AWS_SES_MESSAGE_ID;
    
    public String getSUBJECT() {
        return SUBJECT;
    }

    public void setSUBJECT(String SUBJECT) {
        this.SUBJECT = SUBJECT;
    }

    public String getSENDER_ADDRESS() {
        return SENDER_ADDRESS;
    }

    public void setSENDER_ADDRESS(String SENDER_ADDRESS) {
        this.SENDER_ADDRESS = SENDER_ADDRESS;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "RECIPIENT")
    public Set<String> getRECIPIENTS() {
        return RECIPIENTS;
    }

    public void setRECIPIENTS(Set<String> RECIPIENTS) {
        this.RECIPIENTS = RECIPIENTS;
    }

    @Column(columnDefinition="MEDIUMTEXT")
    public String getBODY() {
        return BODY;
    }

    public void setBODY(String BODY) {
        this.BODY = BODY;
    }
    
    public void addRecipient(String TO) {
        getRECIPIENTS().add(TO);
    }
    
    public void addSingleRecipient(String TO) {
        setRECIPIENTS(new HashSet());
        this.addRecipient(TO);
    }
    
    public void addReplyTo(String replyTo) {
        getREPLY_TO_ADDRESSES().add(replyTo);
    }

    public String getSENDER_NAME() {
        return SENDER_NAME;
    }

    public void setSENDER_NAME(String SENDER_NAME) {
        this.SENDER_NAME = SENDER_NAME;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public Set<String> getREPLY_TO_ADDRESSES() {
        return REPLY_TO_ADDRESSES;
    }

    public void setREPLY_TO_ADDRESSES(Set<String> REPLY_TO_ADDRESSES) {
        this.REPLY_TO_ADDRESSES = REPLY_TO_ADDRESSES;
    }

    public void PROCESSING_STATUS(EMAIL_PROCESSING_STATUS status){
        this.setPROCESSING_STATUS(status.label);
    }

    public int getRETRIES() {
        return RETRIES;
    }

    public void setRETRIES(int RETRIES) {
        this.RETRIES = RETRIES;
    }

    public String getAWS_SES_MESSAGE_ID() {
        return AWS_SES_MESSAGE_ID;
    }

    public void setAWS_SES_MESSAGE_ID(String AWS_SES_MESSAGE_ID) {
        this.AWS_SES_MESSAGE_ID = AWS_SES_MESSAGE_ID;
    }
    
}
