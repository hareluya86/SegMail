/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eds.entity.batch;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.CascadeType;
import static javax.persistence.ConstraintMode.NO_CONSTRAINT;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

/**
 * A stop condition. The absence of this will leave the decision of future runs 
 * to BatchJobSchedule.
 * 
 * @author LeeKiatHaw
 */
@Entity
@Table(name = "BATCH_JOB_CONDTION")
public class BatchJobCondition implements Serializable{
    
    private BatchJob BATCH_JOB;
    
    /**
     * The EJB service name.
     */
    private String SERVICE_NAME;

    /**
     * The EJB service method that should be called to get the boolean result.
     * 
     * True if the next run should be scheduled.
     */
    private String SERVICE_METHOD;
    
    private List<BatchJobConditionParam> PARAMS = new ArrayList<>();
    
    @Id
    @ManyToOne(cascade = {
        //CascadeType.PERSIST,
        CascadeType.MERGE,
        CascadeType.REFRESH
    })
    @JoinColumn(name = "BATCH_JOB",
            referencedColumnName = "BATCH_JOB_ID",
            foreignKey = @ForeignKey(name = "BATCH_JOB", value = NO_CONSTRAINT))
    public BatchJob getBATCH_JOB() {
        return BATCH_JOB;
    }

    public void setBATCH_JOB(BatchJob BATCH_JOB) {
        this.BATCH_JOB = BATCH_JOB;
    }

    public String getSERVICE_NAME() {
        return SERVICE_NAME;
    }

    public void setSERVICE_NAME(String SERVICE_NAME) {
        this.SERVICE_NAME = SERVICE_NAME;
    }

    public String getSERVICE_METHOD() {
        return SERVICE_METHOD;
    }

    public void setSERVICE_METHOD(String SERVICE_METHOD) {
        this.SERVICE_METHOD = SERVICE_METHOD;
    }
    
    public void addPARAMS(BatchJobConditionParam param) {
        this.PARAMS.add(param);
    }
    
    @OneToMany(mappedBy="BATCH_JOB_CONDITION",fetch=FetchType.EAGER)
    @OrderColumn(name = "PARAM_ORDER")
    public List<BatchJobConditionParam> getPARAMS() {
        return PARAMS;
    }

    public void setPARAMS(List<BatchJobConditionParam> PARAMS) {
        this.PARAMS = PARAMS;
    }
    
    public boolean continueNextRun() {
        try {
            int numParams = getPARAMS().size();
            Object[] params = new Object[numParams];

            for (int i = 0; i < numParams; i++) {
                BatchJobConditionParam param = getPARAMS().get(i);

                if (param.getSERIALIZED_OBJECT() != null && !param.getSERIALIZED_OBJECT().isEmpty()) {
                    Object obj = param.SERIALIZED_OBJECT();
                    Class clazz = obj.getClass();
                    params[i] = obj;
                    continue;
                }
                params[i] = param.getSTRING_VALUE();
            }

            Object ejb = InitialContext.doLookup("java:module/" + getSERVICE_NAME());

            Object ret = null;
            Method[] methodArray = ejb.getClass().getMethods();
            for (int i = 0; i < methodArray.length; i++) {
                if (getSERVICE_METHOD().equals(methodArray[i].getName())) {
                    Method method = methodArray[i];
                    ret = method.invoke(ejb, params);
                    break;
                }
            }
            if(ret == null)
                return false;
            if(Boolean.FALSE.equals(ret))
                return false;
            if(ret.getClass().equals(Collection.class)) {
                if (((Collection)ret).isEmpty())
                    return false;
            }
        } catch (IOException | ClassNotFoundException | NamingException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return false;
        }
        
        return true;
    }
}
