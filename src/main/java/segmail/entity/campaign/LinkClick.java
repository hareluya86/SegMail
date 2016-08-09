/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.entity.campaign;

import eds.entity.transaction.EnterpriseTransaction;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author LeeKiatHaw
 */
@Entity
@Table(name="LINK_CLICK")
public class LinkClick extends EnterpriseTransaction {
    
    private String LINK_KEY;

    @NotNull
    public String getLINK_KEY() {
        return LINK_KEY;
    }

    public void setLINK_KEY(String LINK_KEY) {
        this.LINK_KEY = LINK_KEY;
    }
    
    
}
