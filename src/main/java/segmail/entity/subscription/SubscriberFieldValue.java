/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.entity.subscription;

import eds.entity.data.EnterpriseData;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 * @author LeeKiatHaw
 */
@Entity
@Table(name="SUBSCRIBER_FIELD_VALUE")
public class SubscriberFieldValue extends EnterpriseData<SubscriberAccount> {

    //private SubscriptionListField FIELD;
    private String FIELD_NAME;
    private String VALUE;

    public String getFIELD_NAME() {
        return FIELD_NAME;
    }

    public void setFIELD_NAME(String FIELD_NAME) {
        this.FIELD_NAME = FIELD_NAME;
    }

    /*
    public SubscriptionListField getFIELD() {
        return FIELD;
    }

    public void setFIELD(SubscriptionListField FIELD) {
        this.FIELD = FIELD;
    }*/

    public String getVALUE() {
        return VALUE;
    }

    public void setVALUE(String VALUE) {
        this.VALUE = VALUE;
    }
    
    @Override
    public void randInit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object generateKey() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*@Override
    public int compareTo(EnterpriseData o) {
        
        //o is also a SubscriberFieldValue
        if(this.getClass() == o.getClass() && this.getOWNER().equals(o.getOWNER()))
            return this.getFIELD().compareTo(((SubscriberFieldValue)o).getFIELD());
        
        return super.compareTo(o);
    }*/

    @Override
    public String getHTMLName() {
        String htmlName = this.FIELD_NAME.trim().replaceAll(" ", "-");
        return htmlName;
    }
    
    
    
}
