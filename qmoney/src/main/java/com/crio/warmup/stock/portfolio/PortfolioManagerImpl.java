
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private  RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;

  PortfolioManagerImpl( StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }
  @Deprecated 
  protected PortfolioManagerImpl( RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public static double totalNoOfYears( LocalDate startDate,  LocalDate endDate) {
     double totalNoOfDays = ChronoUnit.DAYS.between(startDate, endDate);
     double totalYears = totalNoOfDays / 365;
    return totalYears;

  }

  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException,
      StockQuoteServiceException {

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<AnnualizedReturn> annualizedReturnList = new ArrayList<>();
         
        List<Future<AnnualizedReturn>> futureList = new ArrayList<>();
        for(PortfolioTrade trade: portfolioTrades) {
          Callable<AnnualizedReturn> callableObj = () -> {return getAnnualreturn(trade, endDate);};
          Future<AnnualizedReturn> future = executor.submit(callableObj);
          futureList.add(future);
        }
        
        //convert result to list of annual returns
        for(int i = 0; i < portfolioTrades.size() ; i++) {
          Future<AnnualizedReturn> futureAnnualReturn = futureList.get(i);
          try {
            AnnualizedReturn annualizedReturn = futureAnnualReturn.get();
            annualizedReturnList.add(annualizedReturn);
          } catch (ExecutionException e) {
            
            throw new StockQuoteServiceException("Error while calling API",e);
          }
          
        }

        Comparator<AnnualizedReturn> sortByAnnualReturn = new Comparator<AnnualizedReturn>() {
          public int compare( AnnualizedReturn annualreturn1, AnnualizedReturn annualreturn2) {
            return (int) (annualreturn1.getAnnualizedReturn().compareTo(annualreturn2.getAnnualizedReturn()));
          }
        };
        Collections.sort(annualizedReturnList, sortByAnnualReturn.reversed());
        return annualizedReturnList;
        
        

        
      }

    public AnnualizedReturn getAnnualreturn( PortfolioTrade trade,  LocalDate endDate)
      throws JsonProcessingException, StockQuoteServiceException {
     String symbol = trade.getSymbol();
     LocalDate startDate = trade.getPurchaseDate();
     List<Candle> stocksStartToEnd = getStockQuote(symbol, startDate, endDate);

     Candle initialStock = stocksStartToEnd.get(0);
     Candle endStock = stocksStartToEnd.get(stocksStartToEnd.size() - 1);

     double buyPrice = initialStock.getOpen();
     double sellPrice = endStock.getClose();

    double totalReturns;
    totalReturns = (sellPrice - buyPrice) / buyPrice;

     double totalNoOfYears = totalNoOfYears(startDate, endDate);
     double base = totalReturns + 1;
     double exponent = 1 / totalNoOfYears;
     double annualizedReturns = Math.pow(base, exponent) - 1;
     AnnualizedReturn annualizedReturn = new AnnualizedReturn(
       trade.getSymbol(), annualizedReturns, totalReturns);
    //System.out.println(annualizedReturn);
    return annualizedReturn;

  }

  public AnnualizedReturn calculateAnnualizedReturns( PortfolioTrade trade,  LocalDate endDate)
      throws JsonProcessingException, StockQuoteServiceException {
     String symbol = trade.getSymbol();
     LocalDate startDate = trade.getPurchaseDate();
     List<Candle> stocksStartToEnd = getStockQuote(symbol, startDate, endDate);

     Candle initialStock = stocksStartToEnd.get(0);
     Candle endStock = stocksStartToEnd.get(stocksStartToEnd.size() - 1);

     double buyPrice = initialStock.getOpen();
     double sellPrice = endStock.getClose();

    double totalReturns;
    totalReturns = (sellPrice - buyPrice) / buyPrice;

     double totalNoOfYears = totalNoOfYears(startDate, endDate);
     double base = totalReturns + 1;
     double exponent = 1 / totalNoOfYears;
     double annualizedReturns = Math.pow(base, exponent) - 1;
     AnnualizedReturn annualizedReturn = new AnnualizedReturn(
       trade.getSymbol(), annualizedReturns, totalReturns);
    //System.out.println(annualizedReturn);
    return annualizedReturn;

  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn( List<PortfolioTrade> portfolioTrades,
       LocalDate endDate) throws StockQuoteServiceException {
     List<AnnualizedReturn> annualizedReturnsList = new ArrayList<>();
    for ( PortfolioTrade Trade : portfolioTrades) {

      try {
        annualizedReturnsList.add(calculateAnnualizedReturns(Trade, endDate));
      } catch (JsonProcessingException e) {
        
        e.printStackTrace();
      }

    }
     Comparator<AnnualizedReturn> sortByAnnualReturn = new Comparator<AnnualizedReturn>() {
      public int compare( AnnualizedReturn annualreturn1, AnnualizedReturn annualreturn2) {
        return (int) (annualreturn1.getAnnualizedReturn().compareTo(annualreturn2.getAnnualizedReturn()));
      }
    };
    Collections.sort(annualizedReturnsList, sortByAnnualReturn.reversed());

    return annualizedReturnsList;

  }

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
 

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from
  // main anymore.
  // Copy your code from Module#3
  // PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and ensure it follows the
  // method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required
  // further as our
  // clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command
  // below:
  // ./gradlew test --tests PortfolioManagerTest

  // CHECKSTYLE:OFF

  // private Comparator<AnnualizedReturn> getComparator() {
  // return
  // Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  // }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.

  public List<Candle> getStockQuote( String symbol,  LocalDate from,  LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {
    return stockQuotesService.getStockQuote(symbol, from ,to);

  }

  // protected String buildUri( String symbol,  LocalDate startDate,  LocalDate endDate) {
  //    String token = "641d596bab1f0f2158d2f53747022e50970d827b";
  //    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
  //       + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
  //    String url = uriTemplate.replace("$APIKEY", token).replace("$SYMBOL", symbol)
  //       .
  //   replace("$STARTDATE", startDate.toString()).replace("$ENDDATE", endDate.toString());
  //   return url;    
  // }
}
