/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seca2.program.batch;

import eds.component.batch.BatchProcessingService;
import eds.component.batch.BatchSchedulingService;
import eds.entity.batch.BATCH_JOB_STATUS;
import eds.entity.batch.BatchJob;
import eds.entity.batch.BatchJobStep;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import org.joda.time.DateTime;
import seca2.component.landing.LandingService;
import static seca2.component.landing.ServerNodeType.ERP;
import seca2.entity.landing.ServerInstance;
import seca2.program.Program;

/**
 *
 * @author LeeKiatHaw
 */
public class ProgramBatch extends Program {
    
    private List<BatchJob> batchJobs;
    private List<ServerInstance> servers;
    private Map<String,String> batchJobStatusMapping;
    
    private BatchJob editingBatchJob;
    private BatchJobStep firstAndOnlyStep;

    @EJB LandingService landingService;
    @EJB BatchSchedulingService batchScheduleService;
    @EJB BatchProcessingService batchProcessingService;

    @Override
    public void clearVariables() {
        
    }

    @Override
    public void initProgramParams() {
        
    }

    @Override
    public void initProgram() {
        batchJobStatusMapping = new HashMap<String,String>();
        batchJobStatusMapping.put(BATCH_JOB_STATUS.WAITING.label, "default");
        batchJobStatusMapping.put(BATCH_JOB_STATUS.SCHEDULED.label, "primary");
        batchJobStatusMapping.put(BATCH_JOB_STATUS.IN_PROCESS.label, "info");
        batchJobStatusMapping.put(BATCH_JOB_STATUS.COMPLETED.label, "success");
        batchJobStatusMapping.put(BATCH_JOB_STATUS.FAILED.label, "danger");
        
    }
    
    public List<BatchJob> getBatchJobs() {
        return batchJobs;
    }

    public void setBatchJobs(List<BatchJob> batchJobs) {
        this.batchJobs = batchJobs;
    }

    public Map<String, String> getBatchJobStatusMapping() {
        return batchJobStatusMapping;
    }

    public void setBatchJobStatusMapping(Map<String, String> batchJobStatusMapping) {
        this.batchJobStatusMapping = batchJobStatusMapping;
    }

    public List<ServerInstance> getServers() {
        return servers;
    }

    public void setServers(List<ServerInstance> servers) {
        this.servers = servers;
    }
    
    public void loadServers(){
        setServers(landingService.getServerInstances(ERP));
    }
    
    public void loadBatchJobs(){
        DateTime lowDateTime = new DateTime(1800,1,1,0,0,0,0);
        Timestamp lowTS = new Timestamp(lowDateTime.getMillis());
        DateTime highDateTime = new DateTime(9999,12,31,0,0,0,0);
        Timestamp highTS = new Timestamp(highDateTime.getMillis());
        
        setBatchJobs(batchScheduleService.getBatchJobs(lowTS, highTS, null));
    }

    public BatchJob getEditingBatchJob() {
        return editingBatchJob;
    }

    public void setEditingBatchJob(BatchJob editingBatchJob) {
        this.editingBatchJob = editingBatchJob;
    }

    public BatchJobStep getFirstAndOnlyStep() {
        return firstAndOnlyStep;
    }

    public void setFirstAndOnlyStep(BatchJobStep firstAndOnlyStep) {
        this.firstAndOnlyStep = firstAndOnlyStep;
    }
    
    
}
