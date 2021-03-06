/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package segmail.entity.campaign;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.And;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.FieldExpressionFactory;
import eds.entity.data.EnterpriseData;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import org.joda.time.DateTime;

/**
 *
 * @author LeeKiatHaw
 */
@Entity
@Table(name="CAMPAIGN_ACTIVITY_SCHEDULE")
@EntityListeners({
    CampaignActivityScheduleListener.class
})
public class CampaignActivitySchedule extends EnterpriseData<CampaignActivity> {
    
    public static final int MAX_SEND_IN_BATCH = 50000;
    public static final int MIN_SEND_IN_BATCH = 1;

    protected long SEND_IN_BATCH;
    
    protected String CRON_EXPRESSION;
    
    protected int EVERY_HOUR;
    
    public long getSEND_IN_BATCH() {
        return SEND_IN_BATCH;
    }

    public void setSEND_IN_BATCH(long SEND_IN_BATCH) {
        this.SEND_IN_BATCH = SEND_IN_BATCH;
    }

    public String getCRON_EXPRESSION() {
        return CRON_EXPRESSION;
    }

    public void setCRON_EXPRESSION(String CRON_EXPRESSION) {
        this.CRON_EXPRESSION = CRON_EXPRESSION;
    }

    public int getEVERY_HOUR() {
        return EVERY_HOUR;
    }

    public void setEVERY_HOUR(int EVERY_HOUR) {
        this.EVERY_HOUR = EVERY_HOUR;
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
    public String HTMLName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Generate a cron expression from its own parameters based on time.
     * 
     * @param time
     * @return 
     */
    public CampaignActivitySchedule generateCronExp(DateTime time) {
        
        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))
                //.withYear(FieldExpressionFactory.always()) //Unix has no year
                .withMonth(FieldExpressionFactory.always())
                .withDoM(FieldExpressionFactory.always())
                .withDoW(FieldExpressionFactory.always())
                .withHour(
                        (getEVERY_HOUR()-1 > 0) ? 
                                (
                                        (getEVERY_HOUR() < 24) ? 
                                                //FieldExpressionFactory.every(FieldExpressionFactory.on(time.getHourOfDay()),getEVERY_HOUR())
                                                everyNHourStartingFrom(getEVERY_HOUR(), time.getHourOfDay())
                                                         : 
                                                FieldExpressionFactory.on(time.getHourOfDay())
                                        )
                                : FieldExpressionFactory.always()
                )
                .withMinute(FieldExpressionFactory.on(time.getMinuteOfHour()))
                //.withSecond(FieldExpressionFactory.always()) //Unix has no seconds
                .instance();
        String cronExp = cron.asString();
        setCRON_EXPRESSION(cronExp);
        
        return this;
    }
    
    /**
     * Generate a cron expression from its own parameters based on the local time.
     * 
     * @return 
     */
    public CampaignActivitySchedule generateCronExp() {
        return this.generateCronExp(DateTime.now());
    }
    
    private And everyNHourStartingFrom(int n, int start) {
        //7,14,21,28,35,42,49,56,63,70,77,84,91,98,105
        //24,48,72,96,
        if(n <= 0 || 24 % n != 0)
            throw new RuntimeException("Please choose a factor of 24.");
        
        //int[] hours = new int[0];
        int startHour = start;
        List<FieldExpression> hoursExpressions = new ArrayList<>();
        //hoursExpressions.add(FieldExpressionFactory.on(startHour));
        //startHour = (startHour + n) % 24;
        do {
            //hours[hours.length] = startHour;
            hoursExpressions.add(FieldExpressionFactory.on(startHour));
            startHour = (startHour + n) % 24;
        } while(startHour != start);
        return FieldExpressionFactory.and(hoursExpressions);
    }
}
