package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class AgentFundRefreshPortTest {

	@Test
	public void stockQuoteRefreshPayloadUsesAgentSnakeCaseAndKeepsNullMarket() throws Exception {
	    AgentFundRefreshPort port = new AgentFundRefreshPort();
	    Method method = AgentFundRefreshPort.class.getDeclaredMethod("toStockQuoteRequestItems", List.class);
	    method.setAccessible(true);

	    @SuppressWarnings("unchecked")
	    List<Map<String, Object>> payload = (List<Map<String, Object>>) method.invoke(port, List.of(
	            StockQuoteTargetEntity.builder().stockCode("600000").market("1").build(),
	            StockQuoteTargetEntity.builder().stockCode("000001").market(null).build()));

	    Assert.assertEquals("600000", payload.get(0).get("stock_code"));
	    Assert.assertEquals("1", payload.get(0).get("market"));
	    Assert.assertEquals("000001", payload.get(1).get("stock_code"));
	    Assert.assertTrue(payload.get(1).containsKey("market"));
	    Assert.assertNull(payload.get(1).get("market"));
	    Assert.assertFalse(payload.get(0).containsKey("stockCode"));
	}

}
