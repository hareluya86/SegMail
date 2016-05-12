/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seca2.program.batch;

import eds.component.batch.BatchSchedulingService;
import eds.entity.batch.BatchJob;
import eds.entity.batch.BatchJobRun;
import java.sql.Timestamp;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import seca2.entity.landing.ServerInstance;
import seca2.jsf.custom.messenger.FacesMessenger;
import seca2.program.FormEdit;

/**
 *
 * @author LeeKiatHaw
 */
@RequestScoped
@Named("FormEditJobRun")
public class FormEditJobRun implements FormEdit{
    
    @Inject ProgramBatch program;
    
    @EJB BatchSchedulingService batchScheduleService;

    public void loadBatchJobRun(String runKey){
            List<BatchJobRun> results = batchScheduleService.getJobRunsByKey(runKey);
        if(results != null && !results.isEmpty())
            program.setEditingBatchJobRun(results.get(0));
    }
    
    @Override
    public void saveAndContinue() {
        try {
            batchScheduleService.updateBatchJobRun(program.getEditingBatchJobRun());
        } catch (EJBException ex) {
            FacesMessenger.setFacesMessage(this.getClass().getSimpleName(), FacesMessage.SEVERITY_ERROR, ex.getMessage(), "");
        }
    }

    @Override
    public void saveAndClose() {
        saveAndContinue();
        closeWithoutSaving();
    }

    @Override
    public void closeWithoutSaving() {
        program.refresh();
    }

    /**
     * This is cancelling or stopping a batch job run
     */
    @Override
    public void delete() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public List<ServerInstance> getServers() {
        return program.getServers();
    }

    public void setServers(List<ServerInstance> servers) {
        program.setServers(servers);
    }
    
    public BatchJobRun getEditingBatchJobRun() {
        return program.getEditingBatchJobRun();
    }

    public void setEditingBatchJobRun(BatchJobRun editingBatchJobRun) {
        program.setEditingBatchJobRun(editingBatchJobRun);
    }
    
    public BatchJob getEditingBatchJob() {
        return (program.getEditingBatchJobRun() == null) ? null : program.getEditingBatchJobRun().getBATCH_JOB();
    }

    public void setEditingBatchJob(BatchJob editingBatchJob) {
        program.getEditingBatchJobRun().setBATCH_JOB(editingBatchJob);
    }
    
    public boolean isEditable() {
        return program.isEditable();
    }

    public void setEditable(boolean editable) {
        program.setEditable(editable);
    }
    
    public String getScheduledTime() {
        if(getEditingBatchJobRun() == null)
            return "";
        String timeString = program.timestampToString(getEditingBatchJobRun().getSCHEDULED_TIME());
        return timeString;
    }
    
    public void setScheduledTime(String timeString) {
        if(getEditingBatchJobRun() == null)
            return;
        Timestamp ts = program.stringToTimestamp(timeString);
        this.getEditingBatchJobRun().setSCHEDULED_TIME(ts);
        
    }
}
