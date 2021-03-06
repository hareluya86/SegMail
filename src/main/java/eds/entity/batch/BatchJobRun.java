/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eds.entity.batch;

import eds.entity.batch.run.BatchJobRunCancelled;
import eds.entity.batch.run.BatchJobRunCompleted;
import eds.entity.batch.run.BatchJobRunFailed;
import eds.entity.batch.run.BatchJobRunInProcess;
import eds.entity.batch.run.BatchJobRunQueued;
import eds.entity.batch.run.BatchJobRunScheduled;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.CascadeType;
import static javax.persistence.ConstraintMode.NO_CONSTRAINT;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.joda.time.DateTime;

/**
 *
 * @author LeeKiatHaw
 */
@Entity
@Table(name = "BATCH_JOB_RUN",indexes={
            @Index(name="getNextNJobRuns",
                    columnList="SCHEDULED_TIME")
        })
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@EntityListeners({
    BatchJobRunListener.class
})
public abstract class BatchJobRun implements Serializable {
    
    private BatchJob BATCH_JOB;
    
    private String RUN_KEY;
    
    private java.sql.Timestamp DATETIME_CREATED;
    private java.sql.Timestamp SCHEDULED_TIME;
    private java.sql.Timestamp START_TIME;
    private java.sql.Timestamp END_TIME;
    private java.sql.Timestamp CANCEL_TIME;
    private java.sql.Timestamp QUEUED_TIME;
    
    private String CREATED_BY;
    private String RUN_BY;
    
    private String STATUS;
    
    /**
     * The execution server for a BatchJob can be changed anytime within its 
     * lifecycle. After a couple of runs, the administrator may decide that the 
     * BatchJob should switch server from A to B due to load, then in BatchJob, 
     * it would be updated as B, in the previous completed runs for BatchJobRun
     * it should read A but all future runs will read B.
     */
    private String SERVER_NAME;
    
    
    public BatchJobRun() {
    }

    public BatchJobRun(BatchJobRun run) {
        this(
            run.BATCH_JOB, 
            run.RUN_KEY, 
            run.DATETIME_CREATED, 
            run.SCHEDULED_TIME, 
            run.START_TIME, 
            run.END_TIME, 
            run.CANCEL_TIME, 
            run.QUEUED_TIME, 
            run.CREATED_BY, 
            run.RUN_BY, 
            run.STATUS, 
            run.SERVER_NAME
        );
    }
    
    public BatchJobRun(
            BatchJob BATCH_JOB, 
            String RUN_KEY, 
            Timestamp DATETIME_CREATED, 
            Timestamp SCHEDULED_TIME, 
            Timestamp START_TIME, 
            Timestamp END_TIME, 
            Timestamp CANCEL_TIME, 
            Timestamp QUEUED_TIME, 
            String CREATED_BY, 
            String RUN_BY, 
            String STATUS, 
            String SERVER_NAME) {
        this.BATCH_JOB = BATCH_JOB;
        this.RUN_KEY = RUN_KEY;
        this.DATETIME_CREATED = DATETIME_CREATED;
        this.SCHEDULED_TIME = SCHEDULED_TIME;
        this.START_TIME = START_TIME;
        this.END_TIME = END_TIME;
        this.CANCEL_TIME = CANCEL_TIME;
        this.QUEUED_TIME = QUEUED_TIME;
        this.CREATED_BY = CREATED_BY;
        this.RUN_BY = RUN_BY;
        this.STATUS = STATUS;
        this.SERVER_NAME = SERVER_NAME;
    }

    @ManyToOne(cascade={
        CascadeType.MERGE,
        CascadeType.REFRESH
    })
    @JoinColumn(name = "BATCH_JOB",
            referencedColumnName = "BATCH_JOB_ID",
            foreignKey = @ForeignKey(name = "BATCH_JOB",value=NO_CONSTRAINT))
    public BatchJob getBATCH_JOB() {
        return BATCH_JOB;
    }

    public void setBATCH_JOB(BatchJob BATCH_JOB) {
        this.BATCH_JOB = BATCH_JOB;
    }

    @Id
    public String getRUN_KEY() {
        return RUN_KEY;
    }

    public void setRUN_KEY(String RUN_KEY) {
        this.RUN_KEY = RUN_KEY;
    }

    public Timestamp getSCHEDULED_TIME() {
        return SCHEDULED_TIME;
    }

    public void setSCHEDULED_TIME(Timestamp SCHEDULED_TIME) {
        this.SCHEDULED_TIME = SCHEDULED_TIME;
    }

    public Timestamp getSTART_TIME() {
        return START_TIME;
    }

    public void setSTART_TIME(Timestamp START_TIME) {
        this.START_TIME = START_TIME;
    }

    public Timestamp getEND_TIME() {
        return END_TIME;
    }

    public void setEND_TIME(Timestamp END_TIME) {
        this.END_TIME = END_TIME;
    }

    public String getRUN_BY() {
        return RUN_BY;
    }

    public void setRUN_BY(String RUN_BY) {
        this.RUN_BY = RUN_BY;
    }

    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }
    
    public BATCH_JOB_RUN_STATUS STATUS() {
        return (this.STATUS == null) ? null : BATCH_JOB_RUN_STATUS.valueOf(this.STATUS);
    }
    
    public void STATUS(BATCH_JOB_RUN_STATUS status) {
        this.setSTATUS(status.label);
    }
    
    public Timestamp getDATETIME_CREATED() {
        return DATETIME_CREATED;
    }

    public void setDATETIME_CREATED(Timestamp DATETIME_CREATED) {
        this.DATETIME_CREATED = DATETIME_CREATED;
    }

    public String getCREATED_BY() {
        return CREATED_BY;
    }

    public void setCREATED_BY(String CREATED_BY) {
        this.CREATED_BY = CREATED_BY;
    }

    public Timestamp getCANCEL_TIME() {
        return CANCEL_TIME;
    }

    public void setCANCEL_TIME(Timestamp CANCEL_TIME) {
        this.CANCEL_TIME = CANCEL_TIME;
    }

    public String getSERVER_NAME() {
        return SERVER_NAME;
    }

    public void setSERVER_NAME(String SERVER_NAME) {
        this.SERVER_NAME = SERVER_NAME;
    }

    public Timestamp getQUEUED_TIME() {
        return QUEUED_TIME;
    }

    public void setQUEUED_TIME(Timestamp QUEUED_TIME) {
        this.QUEUED_TIME = QUEUED_TIME;
    }

    //Lifecycle methods
    
    /**
     * Do shit when I say it's time to do shit.
     * 
     * @param changeDateTime 
     */
    public void schedule(DateTime changeDateTime){
        //Create high end date
        DateTime high = new DateTime(9999,12,31,23,59);
        this.setSCHEDULED_TIME(new Timestamp(changeDateTime.getMillis()));
        this.setSTART_TIME(null);
        this.setEND_TIME(new Timestamp(high.getMillis()));
        this.setCANCEL_TIME(null);
        this.setSTATUS(BATCH_JOB_RUN_STATUS.SCHEDULED.label);
    }
    
    /**
     * Do it.
     * 
     * @param changeDateTime 
     */
    public void start(DateTime changeDateTime) {
        //Create high end date
        DateTime high = new DateTime(9999,12,31,23,59);
        this.setSTART_TIME(new Timestamp(changeDateTime.getMillis()));
        this.setEND_TIME(new Timestamp(high.getMillis()));
        this.setCANCEL_TIME(null);
        this.setSTATUS(BATCH_JOB_RUN_STATUS.IN_PROCESS.label);
    }
    
    /**
     * It's off. 
     * 
     * @param changeDateTime 
     */
    public void cancel(DateTime changeDateTime) {
        this.setCANCEL_TIME(new Timestamp(changeDateTime.getMillis()));
        this.setEND_TIME(this.getCANCEL_TIME());
        this.setSTATUS(BATCH_JOB_RUN_STATUS.CANCELLED.label);
    }
    
    /**
     * It's done.
     * 
     * @param changeDateTime 
     */
    public void complete(DateTime changeDateTime) {
        this.setEND_TIME(new Timestamp(changeDateTime.getMillis()));
        this.setCANCEL_TIME(null);
        this.setSTATUS(BATCH_JOB_RUN_STATUS.COMPLETED.label);
    }
    
    public void fail(DateTime changeDateTime) {
        this.setEND_TIME(new Timestamp(changeDateTime.getMillis()));
        this.setCANCEL_TIME(null);
        this.setSTATUS(BATCH_JOB_RUN_STATUS.FAILED.label);
    }
    
    public void queue(DateTime changeDateTime) {
        this.setQUEUED_TIME(new Timestamp(changeDateTime.getMillis()));
        this.setSTATUS(BATCH_JOB_RUN_STATUS.QUEUED.label);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.RUN_KEY);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BatchJobRun other = (BatchJobRun) obj;
        if (!Objects.equals(this.RUN_KEY, other.RUN_KEY)) {
            return false;
        }
        return true;
    }
    
    public BatchJobRun transit(BATCH_JOB_RUN_STATUS newStatus, DateTime dt) {
        // If either old or new statuses are null
        if(newStatus == null || this.STATUS() == null)
            return this;
        
        // If no change in status
        if(newStatus.equals(this.STATUS()))
            return this;
        
        if(newStatus.equals(BATCH_JOB_RUN_STATUS.SCHEDULED))
            return new BatchJobRunScheduled(this,dt);
        
        if(newStatus.equals(BATCH_JOB_RUN_STATUS.QUEUED))
            return new BatchJobRunQueued(this,dt);
        
        if(newStatus.equals(BATCH_JOB_RUN_STATUS.IN_PROCESS))
            return new BatchJobRunInProcess(this,dt);
        
        if(newStatus.equals(BATCH_JOB_RUN_STATUS.COMPLETED))
            return new BatchJobRunCompleted(this,dt);
        
        if(newStatus.equals(BATCH_JOB_RUN_STATUS.FAILED))
            return new BatchJobRunFailed(this,dt);
        
        if(newStatus.equals(BATCH_JOB_RUN_STATUS.CANCELLED))
            return new BatchJobRunCancelled(this,dt);
        
        // If the class was to remain the same, at least change the status
        this.STATUS(newStatus);
        return this;
    }
    
}
