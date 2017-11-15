/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eds.entity.batch.run;

import eds.entity.batch.BatchJobRun;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.joda.time.DateTime;

/**
 *
 * @author LeeKiatHaw
 */
@Entity
@Table(name = "BATCH_JOB_RUN_FAILED")
public class BatchJobRunFailed extends BatchJobRun {

    public BatchJobRunFailed() {
    }

    public BatchJobRunFailed(BatchJobRun run, DateTime scheduledTime) {
        super(run);
        
        this.fail(scheduledTime);
    }
}
