package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  private final RestTemplate restTemplate;

  
  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  


  
  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate startDate, LocalDate endDate)
      throws JsonProcessingException , StockQuoteServiceException
  {
    //CHECKSTYLE:ON
    String response = restTemplate.getForObject(buildUri(symbol, startDate, endDate), String.class);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Candle[] result = objectMapper.readValue(response, TiingoCandle[].class);
    return Arrays.asList(result);
  }


  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token = "d7ee5290251fd4882f10fde8ada179ccc1450745";
    String uri = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
        + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return uri.replace("$APIKEY", token).replace("$SYMBOL", symbol)
        .replace("$STARTDATE", startDate.toString())
        .replace("$ENDDATE", endDate.toString());
  }

}
