package com.dreamyphobic.stockalert.util;

import com.dreamyphobic.stockalert.model.AssertQuote;

import java.util.Map;

public interface RatesUpdateListener {
   void initialUpdate(Map<String,AssertQuote> stringAssertQuoteMap);
   void itemUpdate(AssertQuote assertQuote);
}
