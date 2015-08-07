/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.entity.subscription;

import eds.entity.data.EnterpriseObject;
import eds.entity.mail.MailRecipient;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author LeeKiatHaw
 */
@Entity
@Table(name="SUBSCRIBER")
public class SubscriberAccount extends EnterpriseObject implements MailRecipient {
    
    private String EMAIL;
    private String FIRSTNAME;
    private String LASTNAME;

    public String getEMAIL() {
        return EMAIL;
    }

    public void setEMAIL(String EMAIL) {
        this.EMAIL = EMAIL;
    }

    public String getFIRSTNAME() {
        return FIRSTNAME;
    }

    public void setFIRSTNAME(String FIRSTNAME) {
        this.FIRSTNAME = FIRSTNAME;
    }

    public String getLASTNAME() {
        return LASTNAME;
    }

    public void setLASTNAME(String LASTNAME) {
        this.LASTNAME = LASTNAME;
    }
    
    
    
    @Override
    public void randInit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object generateKey() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String alias() {
        return this.EMAIL;
    }

    @Override
    public String getAddress() {
        return this.EMAIL;
    }

    @Override
    public long getId() {
        return this.OBJECTID;
    }

    @Override
    public void setId(long id) {
        this.OBJECTID = id;
    }

    @Override
    public void setAddress(String address) {
        this.EMAIL = address;
    }

    
    
}