package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.command;


import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class StopHandler extends CommonEventHandler implements CommandHandler  {
  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private final ApplicationController applicationController;
  private final QueueController queueController;

  public StopHandler() {
    Services services = PluginContext.getInstance().getServices();
    this.applicationController = services.getApplicationController();
    this.queueController = services.getQueueController();
  }

  @Override
  public synchronized void handle(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationController.find(domain);
    if (application == null)
      return;

    if (!isPrivateChat(parameters)) {
      final String dialogId = ParamsExtractor.getDialogId(parameters);
      messageDeliveryService.sendMessageToChat(application, dialogId, getLocalizedMessage(parameters,"only.for.private.chats"));
      return;
    }

    Operator operator = getOrCreateOperator(parameters, application);
    Queue queue = queueController.getProcessingQueue(operator);

    if (queue!=null) {
      if (logger.isDebugEnabled()) {
        String operatorFullName = ParamsExtractor.getOperatorFullNameWithEncoding(parameters);
        logger.debug("Operator stop messaging. Application domain: " + domain + ". Operator ID: " + operator.getId() + ". Operator name: " + operatorFullName + ". User ID: " + queue.getUser().getUserId() + ". Service ID: " + queue.getServiceId());
      }
      queueController.removeFromQueue(queue.getApplication(), queue.getUser(), queue.getServiceId());
      messageDeliveryService.sendMessageToUser(queue, getLocalizedMessage(queue.getLanguage(),"operator.quit"));
      messageDeliveryService.sendMessageToOperator(operator, getLocalizedMessage(parameters,"user.flushed")) ;
    } else {
      messageDeliveryService.sendMessageToOperator(operator, getLocalizedMessage(parameters,"user.not.flushed"));
    }
  }
}
