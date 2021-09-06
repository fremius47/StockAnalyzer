
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  private RestTemplate restTemplate;

  private StockQuotesService stockQuotesService;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }



  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF

  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate)
  {
    //CHECKSTYLE:ON

    // Rate limit exception is thrown only in last modules.
    List<AnnualizedReturn> response = new ArrayList<>();
    for (PortfolioTrade trade : portfolioTrades) {
      response.add(getAnnualizedReturn(endDate, trade));
    }
    return response.stream()
        .sorted(getComparator())
        .collect(Collectors.toList());
  }



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.
  private AnnualizedReturn getAnnualizedReturn(LocalDate endDate, PortfolioTrade trade)
  {
    //CHECKSTYLE:ON
    try {

      List<Candle> candles = null;

      String moduleToRun = null;
      moduleToRun = "REFACTOR";



        //CHECKSTYLE:OFF
        candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
        //CHECKSTYLE:ON


      List<Candle> sortedAndFilteredCandles = candles.stream().filter(candle ->
          trade.getPurchaseDate().atStartOfDay().minus(1, SECONDS)
              .isBefore(candle.getDate().atStartOfDay())
              && endDate.plus(1, DAYS).atStartOfDay().isAfter(candle.getDate().atStartOfDay()))
          .sorted(Comparator.comparing(Candle::getDate))
          .collect(Collectors.toList());

      Double buyPrice = sortedAndFilteredCandles.get(0).getOpen();
      Candle lastCandle = sortedAndFilteredCandles.get(sortedAndFilteredCandles.size() - 1);
      Double sellPrice = lastCandle.getClose();

      double totalReturns = ((sellPrice - buyPrice) / buyPrice);
      double years =
          trade.getPurchaseDate().until(lastCandle.getDate(), ChronoUnit.DAYS) / 365.2425d;
      double annualizedReturns = Math.pow((1 + totalReturns), (1 / years)) - 1;
      return new AnnualizedReturn(trade.getSymbol(), annualizedReturns,
          totalReturns);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return new AnnualizedReturn(trade.getSymbol(), Double.NaN, Double.NaN);
    }
  }


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
  throws JsonProcessingException {
// CRIO_SOLUTION_START_MODULE_REFACTOR
String response = restTemplate.getForObject(buildUri(symbol, from, to), String.class);
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.registerModule(new JavaTimeModule());
Candle[] result = objectMapper.readValue(response, TiingoCandle[].class);
if (result == null) {
  return new ArrayList<>();
}
return Arrays.asList(result);
// CRIO_SOLUTION_END_MODULE_REFACTOR
// CRIO_UNCOMMENT_START_MODULE_REFACTOR
// return null;
// CRIO_UNCOMMENT_END_MODULE_REFACTOR
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
