
package com.crio.warmup.stock.quotes;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class AlphaVantageService implements StockQuotesService {



  private final RestTemplate restTemplate;

  protected AlphaVantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate startDate, LocalDate endDate)
      throws JsonProcessingException , StockQuoteServiceException
  {
    //CHECKSTYLE:ON
    String response = restTemplate.getForObject(buildUri(symbol), String.class);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    try {
      AlphavantageDailyResponse result =
          objectMapper.readValue(response, AlphavantageDailyResponse.class);
      return result.getCandles().entrySet().stream()
          .filter(entry -> between(entry.getKey(), startDate, endDate))
          .map(entry -> {
            try {
              entry.getValue().setDate(entry.getKey());
            } catch (Exception e) {
              e.printStackTrace();
            }
            return entry.getValue();
          })
          .sorted(Comparator.comparing(Candle::getDate))
          .collect(Collectors.toList());
    } catch (Exception ex) {

      if (ex instanceof RuntimeException) {
        throw new StockQuoteServiceException("Alphavantage returned invalid response", ex);
      }

      throw ex;
    }
  }

  private boolean between(LocalDate date, LocalDate startDate, LocalDate endDate) {
    return startDate.atStartOfDay().minus(1, SECONDS).isBefore(date.atStartOfDay())
        && endDate.plus(1, DAYS).atStartOfDay().isAfter(date.atStartOfDay());
  }



  protected String buildUri(String symbol) {
    String uri = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED"
        + "&symbol=$SYMBOL&apikey=$APIKEY&outputsize=full";
    return uri.replace("$APIKEY", "9ILGFAZ7HNYOJ96T")
        .replace("$SYMBOL", symbol);
  }


}

