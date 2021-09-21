
package com.crio.warmup.stock.portfolio;


import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;

  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService=stockQuotesService;
    
  }





  @Override
public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
  LocalDate endDate) {

    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> list1=new ArrayList<>();
    for (PortfolioTrade t:portfolioTrades)
    {

    annualizedReturn=getAnnualizedReturn(t,endDate);
    list1.add(annualizedReturn);
    }
    Comparator<AnnualizedReturn>sortBy=Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  Collections.sort(list1,sortBy);
  return list1;
}
@Override
 public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
    List<PortfolioTrade> portfolioTrades,LocalDate endDate, int numThreads) throws InterruptedException, StockQuoteServiceException {
      List<AnnualizedReturn> list1=new ArrayList<>();
      List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Callable<AnnualizedReturn>> callableTasks = new ArrayList<>();
      for (PortfolioTrade t:portfolioTrades)
      {
        Callable<AnnualizedReturn> callableTask = () -> {
          return getAnnualizedReturn(t, endDate);
        };
        Future<AnnualizedReturn> futureReturns = executor.submit(callableTask);
        futureReturnsList.add(futureReturns);

      }
      for (int i = 0; i < portfolioTrades.size(); i++) {
        Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
        try {
          AnnualizedReturn returns = futureReturns.get();
          list1.add(returns);
        } catch (ExecutionException e) {
          throw new StockQuoteServiceException("Error when calling the API", e);
    
        }
      }
      Comparator<AnnualizedReturn>sortBy=Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();

    Collections.sort(list1,sortBy);
    return list1;
  }
   

public AnnualizedReturn getAnnualizedReturn(PortfolioTrade t,LocalDate endLocalDate)
{
  AnnualizedReturn annual=null;
  String symbol=t.getSymbol();
  LocalDate startLocalDate=t.getPurchaseDate();
  try{
       List<Candle> stocks;
       stocks=stockQuotesService.getStockQuote(symbol,startLocalDate,endLocalDate);
       Candle stockStart=stocks.get(0);
       Candle stockEnd=stocks.get(stocks.size()-1);
       double sellPrice=stockEnd.getClose();
       double buyPrice=stockStart.getOpen();
       double totalReturn=(sellPrice-buyPrice)/buyPrice;
       Double yearsBetween=(double)ChronoUnit.DAYS.between(startLocalDate, endLocalDate) / 365;

       double annualized_returns=Math.pow((1+totalReturn),(1/yearsBetween))-1;
       annual=new AnnualizedReturn(symbol, annualized_returns, totalReturn);
  }
  catch (StockQuoteServiceException e)
  {
    System.out.println(e.getMessage());
  } 
  catch(JsonProcessingException e)
  {
    annual=new AnnualizedReturn(symbol,Double.NaN,Double.NaN);
  }
  return annual;
 
}


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException,StockQuoteServiceException {

        return stockQuotesService.getStockQuote(symbol, from, to);
    
  }

  protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token="c8d1e4c7ec5e06a9a4ef63fd85b7a6adb56667f0";  
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
           String url= uriTemplate.replace("$APIKEY",token).replace("$SYMBOL",symbol).replace("$STARTDATE",startDate.toString()).replace("$ENDDATE",endDate.toString());
            return url;
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }


}
