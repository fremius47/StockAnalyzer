
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;

  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    //this.stockquote=stockquote;
  }
  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService=stockQuotesService;
    
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




  // private Comparator<AnnualizedReturn> getComparator() {
  //   return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  // }


  @Override
public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
  LocalDate endDate) {
    //LocalDate date1 = LocalDate. parse(trade.getPurchaseDate());
   // LocalDate date2 = LocalDate. parse(to_char(endDate));
   // Period period = trade.getPurchaseDate(). until(endDate);
    //double daysBetween = period.getDays();
    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> list1=new ArrayList<>();
   // RestTemplate restTemplate = new RestTemplate();
    for (PortfolioTrade t:portfolioTrades)
    {

      // UriComponents uri = UriComponentsBuilder
      // .fromHttpUrl("https://api.tiingo.com/tiingo/daily/{id}/prices?startDate={start_date}&endDate={end_date}&token=c8d1e4c7ec5e06a9a4ef63fd85b7a6adb56667f0" )
      // .buildAndExpand(t.getSymbol(),t.getPurchaseDate().toString(),args[1]);
      /*String urlString =buildUri(t.getSymbol(),t.getPurchaseDate(),endDate);
      TiingoCandle[] tingo=restTemplate.getForObject(urlString,TiingoCandle[].class);
     // list1.add(tingo[tingo.length-1].getDate(),t,tingo[0].getOpen(),tingo[tingo.length-1].getClose());
    
    LocalDate purchaseDate=t.getPurchaseDate();
    LocalDate endDate1=tingo[tingo.length-1].getDate();
    Double yearsBetween=(double)ChronoUnit.DAYS.between(purchaseDate, endDate1) / 365;
    //long noOfDaysBetween = trade.getPurchaseDate().until(endDate,DAYS);
    //int diff = b.get(YEAR) - a.get(YEAR);
    double sellPrice=tingo[tingo.length-1].getClose();
    double buyPrice=tingo[0].getOpen();
    double totalReturn=(sellPrice-buyPrice)/buyPrice;
   // double midreturn=1+totalReturn;
    //double midyear=(double)(1.00/yearsBetween);
    double annualized_returns=Math.pow((1+totalReturn),(1/yearsBetween))-1;
    list1.add(new AnnualizedReturn(t.getSymbol(),annualized_returns,totalReturn));*/
    annualizedReturn=getAnnualizedReturn(t,endDate);
    list1.add(annualizedReturn);
    }
    Comparator<AnnualizedReturn>sortBy=Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  //Collections.sort(list1, AnnualizedReturn.getComparator);
  Collections.sort(list1,sortBy);
  return list1;
}
@Override
 public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
    List<PortfolioTrade> portfolioTrades,LocalDate endDate, int numThreads) throws InterruptedException, StockQuoteServiceException {
   // AnnualizedReturn annualizedReturn;
      List<AnnualizedReturn> list1=new ArrayList<>();
      List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Callable<AnnualizedReturn>> callableTasks = new ArrayList<>();
     // RestTemplate restTemplate = new RestTemplate();
      for (PortfolioTrade t:portfolioTrades)
      {
        // Callable<AnnualizedReturn> sumTask = new Callable<AnnualizedReturn>() {
        //   public AnnualizedReturn call()
        //   {
        //     AnnualizedReturn annualizedReturn=getAnnualizedReturn(t,endDate);
        //     return annualizedReturn;
        //   }
        
        //   };
        //   callableTasks.add(sumTask);
       // PortfolioTrade trade = portfolioTrades.get(i);
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
     // List<Future<AnnualizedReturn>> futures = executor.invokeAll(callableTasks);
      Comparator<AnnualizedReturn>sortBy=Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    //Collections.sort(list1, AnnualizedReturn.getComparator);

    //list1=futures.getClass(AnnualizedReturn);
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
      // double midreturn=1+totalReturn;
       //double midyear=(double)(1.00/yearsBetween);
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

//   //CHECKSTYLE:OFF

//   // TODO: CRIO_TASK_MODULE_REFACTOR
//   //  Extract the logic to call Tiingo third-party APIs to a separate function.
//   //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException,StockQuoteServiceException {

        return stockQuotesService.getStockQuote(symbol, from, to);
    // if(from.compareTo(to)>=0)
    //  throw new RuntimeException();

    //  String url=buildUri(symbol,from,to);
    //  TiingoCandle[] stocksStartToEndDate=restTemplate.getForObject(url, TiingoCandle[].class);
    //  if(stocksStartToEndDate==null)
    //   return new ArrayList<Candle>();
    //   else
    //   {
    //     List<Candle> stocksList=Arrays.asList(stocksStartToEndDate);
    //     return stocksList;
    //   }
    
  }

  protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String token="c8d1e4c7ec5e06a9a4ef63fd85b7a6adb56667f0";  
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
           String url= uriTemplate.replace("$APIKEY",token).replace("$SYMBOL",symbol).replace("$STARTDATE",startDate.toString()).replace("$ENDDATE",endDate.toString());
            return url;
  }

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  // protected PortfolioManagerImpl(RestTemplate restTemplate) {
  //   this.restTemplate = restTemplate;
  // }


 
  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }






  // private Comparator<AnnualizedReturn> getComparator() {
  //   return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  // }



  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.

}
