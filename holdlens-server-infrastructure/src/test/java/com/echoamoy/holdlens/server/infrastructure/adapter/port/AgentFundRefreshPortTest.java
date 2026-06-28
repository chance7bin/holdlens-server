package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class AgentFundRefreshPortTest {

	@Test
	public void stockQuoteRefreshPayloadUsesAgentSnakeCaseAndSkipsBlankMarket() throws Exception {
	    AgentFundRefreshPort port = new AgentFundRefreshPort();
	    Method method = AgentFundRefreshPort.class.getDeclaredMethod("toStockQuoteRequestItems", List.class);
	    method.setAccessible(true);

	    @SuppressWarnings("unchecked")
	    List<Map<String, Object>> payload = (List<Map<String, Object>>) method.invoke(port, List.of(
	            StockQuoteTargetEntity.builder().stockCode(" 600000 ").market(" 1 ").build(),
	            StockQuoteTargetEntity.builder().stockCode("000001").market(null).build(),
	            StockQuoteTargetEntity.builder().stockCode("000002").market(" ").build(),
	            StockQuoteTargetEntity.builder().stockCode(" ").market("0").build()));

	    Assert.assertEquals(1, payload.size());
	    Assert.assertEquals("600000", payload.get(0).get("stock_code"));
	    Assert.assertEquals("1", payload.get(0).get("market"));
	    Assert.assertFalse(payload.get(0).containsKey("stockCode"));
	}

}
