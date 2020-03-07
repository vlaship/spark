package vlaship.spark;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.ReduceFunction;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class SparkApp {

    public static void main(String[] args) {
        try (JavaSparkContext sc = new JavaSparkContext(new SparkConf().setAppName("AppName").setMaster("local"))) {
            var sparkSession = SparkSession.builder().getOrCreate();

            sc.setLogLevel("ERROR");
            Logger.getLogger("org").setLevel(Level.ERROR);
            Logger.getLogger("akka").setLevel(Level.ERROR);
            LogManager.getRootLogger().setLevel(Level.ERROR);

            var version = sc.version();
            log.info("SPARK VERSION = {}", version);

            final Integer reduce = sc.parallelize(IntStream.range(1, 101).boxed().collect(Collectors.toList()))
                    .reduce(Integer::sum);
            log.info("sum 1 to 100 = " + reduce);

            var sumHundred = sparkSession
                    .range(1, 101)
                    .reduce((ReduceFunction<Long>) Long::sum);
            log.info("sum 1 to 100 = {}", sumHundred);

            log.info("Reading from csv file: people-example.csv");
            var persons = SparkSession.builder().getOrCreate()
                    .read()
                    .format("csv")
                    .option("header", "true")
                    .load("people-example.csv")
                    .as("epam.spark.Person");
            persons.show(2);

            SQLContext sqlContext = new SQLContext(sc);
            var as = sqlContext.read()
                    .format("com.databricks.spark.csv")
                    .option("inferSchema", "true")
                    .option("header", "true")
                    .option("delimiter", ",")
                    .load("people-example.csv")
                    .as("vlaship.spark.Person");
            as.show();
            var averageAge = as
                    .agg(functions.avg("age"))
                    .first()
                    .get(0);

            log.info("Average Age: {}", averageAge);
        }
    }
}