package schedule;

import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

public class ScheduleTest {
    public static void main(String[] args) {
        try{
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // and start it off
            scheduler.start();

            scheduler.shutdown();
        }catch (Exception e){

        }
    }
}
