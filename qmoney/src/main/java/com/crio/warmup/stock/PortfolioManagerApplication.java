
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;



public class PortfolioManagerApplication {


  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    String file = args[0];
    String contents = readFileAsString(file);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
    return Stream.of(portfolioTrades).map(PortfolioTrade::getSymbol).collect(Collectors.toList());
 
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);

    String token = "d7ee5290251fd4882f10fde8ada179ccc1450745";
    String uri = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return Arrays.stream(portfolioTrades).map(trade -> {
      String url = uri.replace("$APIKEY", token).replace("$SYMBOL", trade.getSymbol())
          .replace("$STARTDATE", trade.getPurchaseDate().toString())
          .replace("$ENDDATE", endDate.toString());
      TiingoCandle[] tiingoCandles = new RestTemplate().getForObject(url, TiingoCandle[].class);
      return new TotalReturnsDto(trade.getSymbol(), Stream.of(tiingoCandles)
          .filter(candle -> candle.getDate().equals(endDate))
          .findFirst().get().getClose());
    }).sorted(Comparator.comparing(TotalReturnsDto::getClosingPrice))
        .map(TotalReturnsDto::getSymbol)
        .collect(Collectors.toList());
  
  }



  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  private static String readFileAsString(String filename) throws URISyntaxException, IOException {
    return new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()),
        "UTF-8");
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
  throws IOException, URISyntaxException {
String file = args[0];
final LocalDate endDate = LocalDate.parse(args[1]);
String contents = readFileAsString(file);
ObjectMapper objectMapper = getObjectMapper();
PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
final String token = "d7ee5290251fd4882f10fde8ada179ccc1450745";
String uri = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

return Arrays.stream(portfolioTrades).map(trade -> {
  String url = uri.replace("$APIKEY", token).replace("$SYMBOL", trade.getSymbol())
      .replace("$STARTDATE", trade.getPurchaseDate().toString())
      .replace("$ENDDATE", endDate.toString());
  TiingoCandle[] tiingoCandles = new RestTemplate().getForObject(url, TiingoCandle[].class);
  Double buyPrice = 0.0;
  Double sellPrice = 0.0;
  for (TiingoCandle candle : tiingoCandles) {
    if (candle.getDate().equals(trade.getPurchaseDate())) {
      buyPrice = candle.getOpen();
    }
    if (candle.getDate().equals(endDate)) {
      sellPrice = candle.getClose();
    }
  }
  return calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice);
})
    .sorted(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed())
    .collect(Collectors.toList());
}

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    System.out.println(String.format("sell-price = %f, buy-price = %f, days = %s",
        sellPrice, buyPrice, trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS)));
    double totalReturns = ((sellPrice - buyPrice) / buyPrice);
    double years = trade.getPurchaseDate().until(endDate, ChronoUnit.DAYS) / 365.24;
    double annualizedReturns = Math.pow((1 + (totalReturns)), (1 / years)) - 1;
    System.out.println(String.format("total-returns = %f, annual-returns = %f",
        totalReturns, annualizedReturns));
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns,
        totalReturns);
  
  }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
    PortfolioManager portfolioManager =
        PortfolioManagerFactory.getPortfolioManager(new RestTemplate());
    return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }


  public static List<String> debugOutputs() {
    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/opt/criodo/qmoney/me_qmoney/qmoney/"
        + "build/resources/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@7350471";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "29";

    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadFile(args));


    printJsonObject(mainReadQuotes(args));


    printJsonObject(mainCalculateSingleReturn(args));


    printJsonObject(mainCalculateReturnsAfterRefactor(args));
    
  }
}

