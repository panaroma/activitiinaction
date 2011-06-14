package org.bpmnwithactiviti.chapter12.bam;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;

public class EsperStatementsCreator implements ServletContextListener {

	private static final Log log = LogFactory.getLog(EsperStatementsCreator.class);

	public static String REQUESTED_AMOUNT_STATEMENT_NAME = "requestedAmount";
	public static String LOANED_AMOUNT_STATEMENT_NAME = "loanedAmount";
	public static String PROCESS_DURATION_STATEMENT_NAME = "processDuration";
	private EPAdministrator epAdmin;

	@Override
	public void contextDestroyed(ServletContextEvent context) {
		epAdmin.getStatement(REQUESTED_AMOUNT_STATEMENT_NAME).destroy();
		epAdmin.getStatement(LOANED_AMOUNT_STATEMENT_NAME).destroy();
		epAdmin.getStatement(PROCESS_DURATION_STATEMENT_NAME).destroy();
	}

	@Override
	public void contextInitialized(ServletContextEvent context) {
		log.info("Creating Esper statements.");
		Configuration configuration = new Configuration();
		configuration.addEventTypeAutoName("org.bpmnwithactiviti.chapter12.bam.event");
		EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
		epAdmin = epService.getEPAdministrator();
		
		epAdmin.createEPL(
			"select avg(requestedAmount) as avgRequestedAmount, max(requestedAmount) as maxRequestedAmount, sum(requestedAmount) as sumRequestedAmount" +
			" from LoanRequestReceivedEvent.win:length(2)",REQUESTED_AMOUNT_STATEMENT_NAME);
		
		epAdmin.createEPL(
			"select count(*) as numLoans, sum(requestedAmount) as sumLoanedAmount from LoanRequestProcessedEvent(requestApproved=true).win:length(2)",LOANED_AMOUNT_STATEMENT_NAME);

		epAdmin.createEPL( 
			"select avg(endEvent.processedTime - beginEvent.receiveTime) as avgProcessDuration," +
			" max(endEvent.processedTime - beginEvent.receiveTime) as maxProcessDuration" +
			" from pattern [" +
			"	every beginEvent=LoanRequestReceivedEvent" +
			"   -> endEvent=LoanRequestProcessedEvent(processInstanceId=beginEvent.processInstanceId)" +
			" ].win:length(2)",PROCESS_DURATION_STATEMENT_NAME);
	}
}
