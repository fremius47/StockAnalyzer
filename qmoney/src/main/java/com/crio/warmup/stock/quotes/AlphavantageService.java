
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

public class AlphavantageService implements StockQuotesService {

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
  //  to fetch daily adjusted data for last 20 years.
  //  Refer to documentation here: https://www.alphavantage.co/documentation/
  //  --
  //  The implementation of this functions will be doing following tasks:
  //    1. Build the appropriate url to communicate with third-party.
  //       The url should consider startDate and endDate if it is supported by the provider.
  //    2. Perform third-party communication with the url prepared in step#1
  //    3. Map the response and convert the same to List<Candle>
  //    4. If the provider does not support startDate and endDate, then the implementation
  //       should also filter the dates based on startDate and endDate. Make sure that
  //       result contains the records for for startDate and endDate after filtering.
  //    5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
  // Note:
  // 1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
  // 2. Run the tests using command below and make sure it passes:
  //    ./gradlew test --tests AlphavantageServiceTest
  //CHECKSTYLE:OFF
    //CHECKSTYLE:ON
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  1. Write a method to create appropriate url to call Alphavantage service. The method should
  //     be using configurations provided in the {@link @application.properties}.
  //  2. Use this method in #getStockQuote.

  private final RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
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

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  1. Write a method to create appropriate url to call Alphavantage service. The method should
  //     be using configurations provided in the {@link @application.properties}.
  //  2. Use this method in #getStockQuote.

  protected String buildUri(String symbol) {
    String uri = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED"
        + "&symbol=$SYMBOL&apikey=$APIKEY&outputsize=full";
    return uri.replace("$APIKEY", "9ILGFAZ7HNYOJ96T")
        .replace("$SYMBOL", symbol);
  }


}

